package com.rf.ranking.integration;

import com.rf.ranking.domain.RepositoryCandidate;
import com.rf.ranking.exception.GitHubException;
import com.rf.ranking.service.RepositorySearchClient;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class GitHubRepositorySearchClient implements RepositorySearchClient {

  private final String baseUrl;
  private final String apiVersion;
  private final String token;
  private final RestTemplate restTemplate;
  private final int maxAttempts;

  public GitHubRepositorySearchClient(
      @Value("${github.base-url:https://api.github.com}") String baseUrl,
      @Value("${github.api-version:2026-03-10}") String apiVersion,
      @Value("${github.token:#{null}}") String token,
      @Qualifier("githubRestTemplate") RestTemplate restTemplate,
      @Value("${github.retry.max-attempts:3}") int maxAttempts
  ) {
    this.baseUrl = baseUrl;
    this.apiVersion = apiVersion;
    this.token = token;
    this.restTemplate = restTemplate;
    this.maxAttempts = Math.max(1, maxAttempts);
  }

  @Override
  public RepositorySearchResult search(String language, LocalDate createdAfter, int page, int limit) {
    try {
      String query = buildQuery(language, createdAfter);
      URI url = buildUrl(query, page, limit);

      var response = executeWithRetries(url);
      if (response == null) {
        throw new GitHubException(
            "INVALID_GITHUB_RESPONSE",
            "GitHub returned null response",
            502
        );
      }

      List<RepositoryCandidate> repositories = response.items() != null
          ? response.items().stream()
              .map(GitHubRepositoryDto::toDomain)
              .toList()
          : List.of();

      return new RepositorySearchResult(repositories, response.totalCount(), response.incompleteResults());
    } catch (GitHubException e) {
      throw e;
    } catch (HttpClientErrorException e) {
      handleHttpClientError(e);
      throw new GitHubException(
          "INVALID_GITHUB_RESPONSE",
          "GitHub returned an invalid or unsupported response",
          502,
          e
      );
    } catch (HttpServerErrorException e) {
      throw new GitHubException(
          "GITHUB_UNAVAILABLE",
          "GitHub is temporarily unavailable",
          503,
          e
      );
    } catch (ResourceAccessException e) {
      throw new GitHubException(
          "GITHUB_UNAVAILABLE",
          "GitHub is temporarily unavailable",
          503,
          e
      );
    } catch (RestClientException e) {
      throw new GitHubException(
          "INVALID_GITHUB_RESPONSE",
          "Failed to parse GitHub response",
          502,
          e
      );
    } catch (RuntimeException e) {
      throw new GitHubException(
          "INVALID_GITHUB_RESPONSE",
          "GitHub returned an invalid or unsupported response",
          502,
          e
      );
    }
  }

  private GitHubSearchResponseDto executeWithRetries(URI url) {
    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
        var responseEntity = restTemplate.exchange(
            url,
            HttpMethod.GET,
            new HttpEntity<>(buildHeaders()),
            GitHubSearchResponseDto.class
        );
        return responseEntity.getBody();
      } catch (HttpServerErrorException | ResourceAccessException e) {
        if (attempt == maxAttempts) {
          throw e;
        }
      }
    }

    throw new IllegalStateException("Retry loop completed without returning or throwing");
  }

  private void handleHttpClientError(HttpClientErrorException e) {
    if (isRateLimited(e)) {
      throw new GitHubException(
          "GITHUB_RATE_LIMITED",
          "GitHub API rate limit exceeded",
          429,
          calculateRetryAfter(e)
      );
    }
  }

  private boolean isRateLimited(HttpClientErrorException e) {
    int status = e.getStatusCode().value();
    if (status == 429) {
      return true;
    }
    if (status != 403 || e.getResponseHeaders() == null) {
      return false;
    }
    return "0".equals(e.getResponseHeaders().getFirst("X-RateLimit-Remaining"));
  }

  private Integer calculateRetryAfter(HttpClientErrorException e) {
    if (e.getResponseHeaders() == null) {
      return 60;
    }

    Integer retryAfter = parsePositiveInt(e.getResponseHeaders().getFirst("Retry-After"));
    if (retryAfter != null) {
      return retryAfter;
    }

    Integer resetEpochSeconds = parsePositiveInt(e.getResponseHeaders().getFirst("X-RateLimit-Reset"));
    if (resetEpochSeconds != null) {
      long waitSeconds = resetEpochSeconds - Instant.now().getEpochSecond();
      return (int) Math.max(1, Math.min(waitSeconds, Integer.MAX_VALUE));
    }

    return 60;
  }

  private Integer parsePositiveInt(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      int parsed = Integer.parseInt(value.trim());
      return parsed > 0 ? parsed : null;
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private String buildQuery(String language, LocalDate createdAfter) {
    String languageQuery = "language:" + language;
    String dateQuery = "created:>=" + createdAfter;
    return languageQuery + " " + dateQuery;
  }

  private HttpHeaders buildHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(List.of(MediaType.valueOf("application/vnd.github+json")));
    headers.set(HttpHeaders.USER_AGENT, "repository-ranking-service/1.0");
    headers.set("X-GitHub-Api-Version", apiVersion);
    if (token != null && !token.isBlank()) {
      headers.setBearerAuth(token);
    }
    return headers;
  }

  private URI buildUrl(String query, int page, int limit) {
    return URI.create(baseUrl + "/search/repositories"
        + "?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
        + "&sort=stars"
        + "&order=desc"
        + "&page=" + page
        + "&per_page=" + limit);
  }
}

package com.redcare.pharmacy.ranking.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redcare.pharmacy.ranking.domain.RepositoryCandidate;
import com.redcare.pharmacy.ranking.exception.GitHubException;
import com.redcare.pharmacy.ranking.service.RepositorySearchClient;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class GitHubRepositorySearchClient implements RepositorySearchClient {

  private final String baseUrl;
  private final String token;
  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;

  public GitHubRepositorySearchClient(
      @Value("${github.base-url:https://api.github.com}") String baseUrl,
      @Value("${github.token:#{null}}") String token
  ) {
    this.baseUrl = baseUrl;
    this.token = token;
    this.restTemplate = new RestTemplate();
    this.objectMapper = new ObjectMapper();
  }

  @Override
  public RepositorySearchResult search(String language, LocalDate createdAfter, int page, int limit) {
    try {
      String query = buildQuery(language, createdAfter);
      String url = buildUrl(query, page, limit);

      var response = restTemplate.getForObject(url, GitHubSearchResponseDto.class);
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
    } catch (org.springframework.web.client.HttpClientErrorException e) {
      handleHttpClientError(e);
      throw new GitHubException(
          "INVALID_GITHUB_RESPONSE",
          "GitHub returned error: " + e.getStatusCode(),
          e.getStatusCode().value()
      );
    } catch (org.springframework.web.client.HttpServerErrorException e) {
      throw new GitHubException(
          "GITHUB_UNAVAILABLE",
          "GitHub server error: " + e.getStatusCode(),
          e.getStatusCode().value()
      );
    } catch (org.springframework.web.client.ResourceAccessException e) {
      throw new GitHubException(
          "GITHUB_UNAVAILABLE",
          "Failed to reach GitHub: " + e.getMessage(),
          503,
          e
      );
    } catch (Exception e) {
      if (e.getMessage() != null && e.getMessage().contains("JSON")) {
        throw new GitHubException(
            "INVALID_GITHUB_RESPONSE",
            "Failed to parse GitHub response",
            502,
            e
        );
      }
      throw new GitHubException(
          "INVALID_GITHUB_RESPONSE",
          "Unexpected error communicating with GitHub",
          502,
          e
      );
    }
  }

  private void handleHttpClientError(org.springframework.web.client.HttpClientErrorException e) {
    if (e.getStatusCode().value() == 429) {
      String retryAfter = e.getResponseHeaders() != null
          ? e.getResponseHeaders().getFirst("Retry-After")
          : null;
      Integer retrySeconds = retryAfter != null ? Integer.parseInt(retryAfter) : null;
      throw new GitHubException(
          "GITHUB_RATE_LIMITED",
          "GitHub API rate limit exceeded",
          429,
          retrySeconds
      );
    }
  }

  private String buildQuery(String language, LocalDate createdAfter) {
    String languageQuery = "language:" + URLEncoder.encode(language, StandardCharsets.UTF_8);
    String dateQuery = "created:>=" + createdAfter;
    return languageQuery + " " + dateQuery;
  }

  private String buildUrl(String query, int page, int limit) {
    return baseUrl + "/search/repositories"
        + "?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
        + "&sort=stars"
        + "&order=desc"
        + "&page=" + page
        + "&per_page=" + limit;
  }
}

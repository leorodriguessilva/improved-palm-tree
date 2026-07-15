package com.rf.ranking.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.rf.ranking.exception.GitHubException;
import com.rf.ranking.service.RepositorySearchClient;
import java.net.SocketTimeoutException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class GitHubRepositorySearchClientTest {

  @Test
  void searchRepositoriesUsesGitHubApiHeadersAndQueryParameters() {
    var restTemplate = new RestTemplate();
    var client = newClient("2026-03-10", "github-token", restTemplate, 3);
    var server = bindServer(restTemplate);

    server.expect(once(), requestTo("https://api.github.test/search/repositories"
            + "?q=language%3AC%2B%2B+created%3A%3E%3D2026-01-01"
            + "&sort=stars&order=desc&page=2&per_page=10"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("User-Agent", "repository-ranking-service/1.0"))
        .andExpect(header("X-GitHub-Api-Version", "2026-03-10"))
        .andExpect(header("Authorization", "Bearer github-token"))
        .andRespond(withSuccess("""
            {
              "total_count": 0,
              "incomplete_results": false,
              "items": []
            }
            """, MediaType.APPLICATION_JSON));

    var result = client.search("C++", LocalDate.parse("2026-01-01"), 2, 10);

    assertThat(result.repositories()).isEmpty();
    assertThat(result.totalCount()).isEqualTo(0);
    assertThat(result.incompleteResults()).isFalse();
    server.verify();
  }

  @Test
  void searchRepositoriesOmitsAuthorizationHeaderWhenTokenIsBlank() {
    var restTemplate = new RestTemplate();
    var client = newClient("2022-11-28", "", restTemplate, 3);
    var server = bindServer(restTemplate);

    server.expect(once(), requestTo("https://api.github.test/search/repositories"
            + "?q=language%3AJava+created%3A%3E%3D2026-01-01"
            + "&sort=stars&order=desc&page=1&per_page=5"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("User-Agent", "repository-ranking-service/1.0"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andExpect(headerDoesNotExist("Authorization"))
        .andRespond(withSuccess("""
            {
              "total_count": 0,
              "incomplete_results": false,
              "items": []
            }
            """, MediaType.APPLICATION_JSON));

    var result = client.search("Java", LocalDate.parse("2026-01-01"), 1, 5);

    assertThat(result.repositories()).isEmpty();
    server.verify();
  }

  @Test
  void searchRepositoriesRetriesTransientServerErrorsAndReturnsSuccessfulAttempt() {
    var restTemplate = new RestTemplate();
    var client = newClient("2026-03-10", "", restTemplate, 3);
    var server = bindServer(restTemplate);
    var url = "https://api.github.test/search/repositories"
        + "?q=language%3AJava+created%3A%3E%3D2026-01-01"
        + "&sort=stars&order=desc&page=1&per_page=5";

    server.expect(once(), requestTo(url)).andRespond(withServerError());
    server.expect(once(), requestTo(url)).andRespond(withServerError());
    server.expect(once(), requestTo(url)).andRespond(withSuccess("""
        {
          "total_count": 0,
          "incomplete_results": false,
          "items": []
        }
        """, MediaType.APPLICATION_JSON));

    var result = client.search("Java", LocalDate.parse("2026-01-01"), 1, 5);

    assertThat(result.repositories()).isEmpty();
    server.verify();
  }

  @Test
  void searchRepositoriesStopsAfterThreeTransientTimeoutFailures() {
    var restTemplate = new RestTemplate();
    var client = newClient("2026-03-10", "", restTemplate, 3);
    var server = bindServer(restTemplate);
    var url = "https://api.github.test/search/repositories"
        + "?q=language%3AJava+created%3A%3E%3D2026-01-01"
        + "&sort=stars&order=desc&page=1&per_page=5";

    server.expect(times(3), requestTo(url))
        .andRespond(withException(new SocketTimeoutException("read timed out")));

    assertThatThrownBy(() -> client.search("Java", LocalDate.parse("2026-01-01"), 1, 5))
        .isInstanceOfSatisfying(GitHubException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo("GITHUB_UNAVAILABLE");
          assertThat(exception.getHttpStatus()).isEqualTo(503);
        });
    server.verify();
  }

  @Test
  void searchRepositoriesDoesNotRetryClientErrors() {
    var restTemplate = new RestTemplate();
    var client = newClient("2026-03-10", "", restTemplate, 3);
    var server = bindServer(restTemplate);
    var url = "https://api.github.test/search/repositories"
        + "?q=language%3AJava+created%3A%3E%3D2026-01-01"
        + "&sort=stars&order=desc&page=1&per_page=5";

    server.expect(once(), requestTo(url)).andRespond(withStatus(HttpStatus.BAD_REQUEST));

    assertThatThrownBy(() -> client.search("Java", LocalDate.parse("2026-01-01"), 1, 5))
        .isInstanceOfSatisfying(GitHubException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo("INVALID_GITHUB_RESPONSE");
          assertThat(exception.getHttpStatus()).isEqualTo(502);
        });
    server.verify();
  }

  @Test
  void searchRepositoriesMaps429WithRetryAfterToLocalRateLimit() {
    var restTemplate = new RestTemplate();
    var client = newClient("2026-03-10", "", restTemplate, 3);
    var server = bindServer(restTemplate);
    var headers = new HttpHeaders();
    headers.set("Retry-After", "120");

    server.expect(once(), requestTo("https://api.github.test/search/repositories"
            + "?q=language%3AJava+created%3A%3E%3D2026-01-01"
            + "&sort=stars&order=desc&page=1&per_page=5"))
        .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).headers(headers));

    assertThatThrownBy(() -> client.search("Java", LocalDate.parse("2026-01-01"), 1, 5))
        .isInstanceOfSatisfying(GitHubException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo("GITHUB_RATE_LIMITED");
          assertThat(exception.getHttpStatus()).isEqualTo(429);
          assertThat(exception.getRetryAfter()).isEqualTo(120);
        });
    server.verify();
  }

  @Test
  void searchRepositoriesMaps403WithRemainingZeroToLocalRateLimit() {
    var restTemplate = new RestTemplate();
    var client = newClient("2026-03-10", "", restTemplate, 3);
    var server = bindServer(restTemplate);
    var headers = new HttpHeaders();
    headers.set("X-RateLimit-Remaining", "0");
    headers.set("X-RateLimit-Reset", String.valueOf(java.time.Instant.now().plusSeconds(90).getEpochSecond()));

    server.expect(once(), requestTo("https://api.github.test/search/repositories"
            + "?q=language%3AJava+created%3A%3E%3D2026-01-01"
            + "&sort=stars&order=desc&page=1&per_page=5"))
        .andRespond(withStatus(HttpStatus.FORBIDDEN).headers(headers));

    assertThatThrownBy(() -> client.search("Java", LocalDate.parse("2026-01-01"), 1, 5))
        .isInstanceOfSatisfying(GitHubException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo("GITHUB_RATE_LIMITED");
          assertThat(exception.getHttpStatus()).isEqualTo(429);
          assertThat(exception.getRetryAfter()).isBetween(1, 90);
        });
    server.verify();
  }

  @Test
  void searchRepositoriesMaps403WithoutRateLimitIndicationToBadGateway() {
    var restTemplate = new RestTemplate();
    var client = newClient("2026-03-10", "", restTemplate, 3);
    var server = bindServer(restTemplate);

    server.expect(once(), requestTo("https://api.github.test/search/repositories"
            + "?q=language%3AJava+created%3A%3E%3D2026-01-01"
            + "&sort=stars&order=desc&page=1&per_page=5"))
        .andRespond(withStatus(HttpStatus.FORBIDDEN));

    assertThatThrownBy(() -> client.search("Java", LocalDate.parse("2026-01-01"), 1, 5))
        .isInstanceOfSatisfying(GitHubException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo("INVALID_GITHUB_RESPONSE");
          assertThat(exception.getHttpStatus()).isEqualTo(502);
        });
    server.verify();
  }

  @Test
  void searchRepositoriesUsesFallbackRetryAfterForInvalidRateLimitHeaders() {
    var restTemplate = new RestTemplate();
    var client = newClient("2026-03-10", "", restTemplate, 3);
    var server = bindServer(restTemplate);
    var headers = new HttpHeaders();
    headers.set("Retry-After", "not-a-number");

    server.expect(once(), requestTo("https://api.github.test/search/repositories"
            + "?q=language%3AJava+created%3A%3E%3D2026-01-01"
            + "&sort=stars&order=desc&page=1&per_page=5"))
        .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).headers(headers));

    assertThatThrownBy(() -> client.search("Java", LocalDate.parse("2026-01-01"), 1, 5))
        .isInstanceOfSatisfying(GitHubException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo("GITHUB_RATE_LIMITED");
          assertThat(exception.getRetryAfter()).isEqualTo(60);
        });
    server.verify();
  }

  @Test
  void emptyItemsAreParsed() {
    var result = new RepositorySearchClient.RepositorySearchResult(
        java.util.List.of(),
        0,
        false
    );

    assertThat(result.repositories()).isEmpty();
    assertThat(result.totalCount()).isEqualTo(0);
  }

  @Test
  void resultsAreParsed() {
    var candidate = new com.rf.ranking.domain.RepositoryCandidate(
        123L,
        "example",
        "owner/example",
        "https://github.com/owner/example",
        "Example repository",
        "Java",
        java.time.Instant.parse("2022-03-01T10:00:00Z"),
        java.time.Instant.parse("2026-07-14T10:00:00Z"),
        1000,
        100
    );

    var result = new RepositorySearchClient.RepositorySearchResult(
        java.util.List.of(candidate),
        1,
        false
    );

    assertThat(result.repositories()).hasSize(1);
    assertThat(result.repositories().get(0).name()).isEqualTo("example");
    assertThat(result.totalCount()).isEqualTo(1);
    assertThat(result.incompleteResults()).isFalse();
  }

  private GitHubRepositorySearchClient newClient(
      String apiVersion,
      String token,
      RestTemplate restTemplate,
      int maxAttempts
  ) {
    return new GitHubRepositorySearchClient(
        "https://api.github.test",
        apiVersion,
        token,
        restTemplate,
        maxAttempts
    );
  }

  private MockRestServiceServer bindServer(RestTemplate restTemplate) {
    return MockRestServiceServer.bindTo(restTemplate).build();
  }
}

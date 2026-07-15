package com.redcare.pharmacy.ranking.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.redcare.pharmacy.ranking.service.RepositorySearchClient;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class GitHubRepositorySearchClientTest {

  @Test
  void searchRepositoriesUsesGitHubApiHeadersAndQueryParameters() {
    var client = new GitHubRepositorySearchClient(
        "https://api.github.test",
        "2026-03-10",
        "github-token"
    );
    var server = bindServer(client);

    server.expect(once(), requestTo("https://api.github.test/search/repositories"
            + "?q=language%3AC%2B%2B+created%3A%3E%3D2026-01-01"
            + "&sort=stars&order=desc&page=2&per_page=10"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("Accept", "application/vnd.github+json"))
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
    var client = new GitHubRepositorySearchClient(
        "https://api.github.test",
        "2022-11-28",
        ""
    );
    var server = bindServer(client);

    server.expect(once(), requestTo("https://api.github.test/search/repositories"
            + "?q=language%3AJava+created%3A%3E%3D2026-01-01"
            + "&sort=stars&order=desc&page=1&per_page=5"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("Accept", "application/vnd.github+json"))
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
    var candidate = new com.redcare.pharmacy.ranking.domain.RepositoryCandidate(
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

  private MockRestServiceServer bindServer(GitHubRepositorySearchClient client) {
    RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
    return MockRestServiceServer.bindTo(restTemplate).build();
  }
}

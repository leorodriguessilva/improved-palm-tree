package com.redcare.pharmacy.ranking.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.redcare.pharmacy.ranking.service.RepositorySearchClient;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class GitHubRepositorySearchClientTest {

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
}

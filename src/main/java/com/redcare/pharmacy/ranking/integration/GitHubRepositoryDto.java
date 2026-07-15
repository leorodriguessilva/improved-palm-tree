package com.redcare.pharmacy.ranking.integration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.redcare.pharmacy.ranking.domain.RepositoryCandidate;
import java.time.Instant;

public record GitHubRepositoryDto(
    long id,
    String name,
    @JsonProperty("full_name")
    String fullName,
    @JsonProperty("html_url")
    String htmlUrl,
    String description,
    String language,
    @JsonProperty("created_at")
    Instant createdAt,
    @JsonProperty("updated_at")
    Instant updatedAt,
    @JsonProperty("stargazers_count")
    int stargazersCount,
    @JsonProperty("forks_count")
    int forksCount
) {

  public RepositoryCandidate toDomain() {
    return new RepositoryCandidate(
        id,
        name,
        fullName,
        htmlUrl,
        description,
        language,
        createdAt,
        updatedAt,
        stargazersCount,
        forksCount
    );
  }
}

package com.rf.ranking.integration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rf.ranking.domain.RepositoryCandidate;
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
    if (updatedAt == null) {
      throw new IllegalArgumentException("GitHub repository updated_at is required");
    }
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

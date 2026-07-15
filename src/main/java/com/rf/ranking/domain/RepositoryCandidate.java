package com.rf.ranking.domain;

import java.time.Instant;

public record RepositoryCandidate(
    long id,
    String name,
    String fullName,
    String url,
    String description,
    String language,
    Instant createdAt,
    Instant updatedAt,
    int stars,
    int forks
) {

  public RepositoryCandidate {
    if (stars < 0) {
      throw new IllegalArgumentException("stars must be non-negative");
    }
    if (forks < 0) {
      throw new IllegalArgumentException("forks must be non-negative");
    }
  }
}

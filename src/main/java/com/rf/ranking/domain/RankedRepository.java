package com.rf.ranking.domain;

import java.time.Instant;

public record RankedRepository(
    long id,
    String name,
    String fullName,
    String url,
    String description,
    String language,
    Instant createdAt,
    Instant updatedAt,
    int stars,
    int forks,
    ScoreBreakdown score,
    int rank
) {

  public RankedRepository {
    if (rank < 1) {
      throw new IllegalArgumentException("rank must be >= 1");
    }
  }

  public static RankedRepository from(RepositoryCandidate candidate, ScoreBreakdown score, int rank) {
    return new RankedRepository(
        candidate.id(),
        candidate.name(),
        candidate.fullName(),
        candidate.url(),
        candidate.description(),
        candidate.language(),
        candidate.createdAt(),
        candidate.updatedAt(),
        candidate.stars(),
        candidate.forks(),
        score,
        rank
    );
  }
}

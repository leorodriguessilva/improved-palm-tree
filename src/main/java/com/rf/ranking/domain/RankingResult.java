package com.rf.ranking.domain;

import java.time.Instant;
import java.util.List;

public record RankingResult(
    RankingRequest request,
    ScoreVersion scoreVersion,
    Instant generatedAt,
    int totalCount,
    boolean incompleteResults,
    List<RankedRepository> repositories
) {

  public RankingResult {
    if (request == null) {
      throw new IllegalArgumentException("request must not be null");
    }
    if (scoreVersion == null) {
      throw new IllegalArgumentException("scoreVersion must not be null");
    }
    if (generatedAt == null) {
      throw new IllegalArgumentException("generatedAt must not be null");
    }
    if (repositories == null) {
      throw new IllegalArgumentException("repositories must not be null");
    }
  }
}

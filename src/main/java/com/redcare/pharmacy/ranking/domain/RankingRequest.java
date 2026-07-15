package com.redcare.pharmacy.ranking.domain;

import java.time.LocalDate;

public record RankingRequest(
    String language,
    LocalDate createdAfter,
    int page,
    int limit,
    ScoreVersion scoreVersion
) {

  public RankingRequest {
    if (language == null || language.isBlank()) {
      throw new IllegalArgumentException("language must not be blank");
    }
    if (createdAfter == null) {
      throw new IllegalArgumentException("createdAfter must not be null");
    }
    if (page < 1) {
      throw new IllegalArgumentException("page must be >= 1");
    }
    if (limit < 1) {
      throw new IllegalArgumentException("limit must be >= 1");
    }
    if (scoreVersion == null) {
      throw new IllegalArgumentException("scoreVersion must not be null");
    }
  }
}

package com.rf.ranking.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rf.ranking.domain.RankedRepository;
import java.time.Instant;

public record RankedRepositoryDto(
    long id,
    String name,
    @JsonProperty("fullName")
    String fullName,
    String url,
    String description,
    String language,
    @JsonProperty("createdAt")
    Instant createdAt,
    @JsonProperty("updatedAt")
    Instant updatedAt,
    int stars,
    int forks,
    ScoreBreakdownDto score,
    int rank
) {

  public static RankedRepositoryDto from(RankedRepository repository) {
    return new RankedRepositoryDto(
        repository.id(),
        repository.name(),
        repository.fullName(),
        repository.url(),
        repository.description(),
        repository.language(),
        repository.createdAt(),
        repository.updatedAt(),
        repository.stars(),
        repository.forks(),
        new ScoreBreakdownDto(
            repository.score().total(),
            repository.score().starsContribution(),
            repository.score().forksContribution(),
            repository.score().recencyContribution()
        ),
        repository.rank()
    );
  }

  public record ScoreBreakdownDto(
      double total,
      @JsonProperty("starsContribution")
      double starsContribution,
      @JsonProperty("forksContribution")
      double forksContribution,
      @JsonProperty("recencyContribution")
      double recencyContribution
  ) {}
}

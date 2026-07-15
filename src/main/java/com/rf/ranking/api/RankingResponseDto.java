package com.rf.ranking.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rf.ranking.domain.RankingResult;
import java.time.Instant;
import java.util.List;

public record RankingResponseDto(
    QueryInfoDto query,
    @JsonProperty("scoreVersion")
    String scoreVersion,
    @JsonProperty("generatedAt")
    Instant generatedAt,
    PaginationInfoDto pagination,
    List<RankedRepositoryDto> repositories
) {

  public static RankingResponseDto from(RankingResult result) {
    return new RankingResponseDto(
        new QueryInfoDto(
            result.request().language(),
            result.request().createdAfter(),
            result.request().page(),
            result.request().limit()
        ),
        result.scoreVersion().getValue(),
        result.generatedAt(),
        new PaginationInfoDto(
            result.request().page(),
            result.request().limit(),
            result.repositories().size(),
            result.totalCount(),
            result.incompleteResults()
        ),
        result.repositories().stream()
            .map(RankedRepositoryDto::from)
            .toList()
    );
  }

  public record QueryInfoDto(
      String language,
      java.time.LocalDate createdAfter,
      int page,
      int limit
  ) {}

  public record PaginationInfoDto(
      int page,
      int limit,
      @JsonProperty("returnedCount")
      int returnedCount,
      @JsonProperty("githubTotalCount")
      int githubTotalCount,
      @JsonProperty("incompleteResults")
      boolean incompleteResults
  ) {}
}

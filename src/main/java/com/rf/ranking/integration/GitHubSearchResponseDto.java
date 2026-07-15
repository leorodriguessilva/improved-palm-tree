package com.rf.ranking.integration;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record GitHubSearchResponseDto(
    @JsonProperty("total_count")
    int totalCount,
    @JsonProperty("incomplete_results")
    boolean incompleteResults,
    List<GitHubRepositoryDto> items
) {}

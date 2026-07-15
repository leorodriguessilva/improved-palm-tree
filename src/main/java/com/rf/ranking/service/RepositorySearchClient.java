package com.rf.ranking.service;

import com.rf.ranking.domain.RepositoryCandidate;
import java.time.LocalDate;
import java.util.List;

public interface RepositorySearchClient {

  RepositorySearchResult search(String language, LocalDate createdAfter, int page, int limit);

  record RepositorySearchResult(
      List<RepositoryCandidate> repositories,
      int totalCount,
      boolean incompleteResults
  ) {}
}

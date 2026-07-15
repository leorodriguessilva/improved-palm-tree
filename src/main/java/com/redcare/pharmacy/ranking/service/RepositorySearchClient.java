package com.redcare.pharmacy.ranking.service;

import com.redcare.pharmacy.ranking.domain.RepositoryCandidate;
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

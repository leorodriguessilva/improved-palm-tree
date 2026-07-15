package com.redcare.pharmacy.ranking.service;

import com.redcare.pharmacy.ranking.domain.RankingRequest;
import com.redcare.pharmacy.ranking.domain.RankingResult;

public interface RepositoryRankingService {

  RankingResult rank(RankingRequest request);
}

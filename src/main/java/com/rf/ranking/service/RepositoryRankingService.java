package com.rf.ranking.service;

import com.rf.ranking.domain.RankingRequest;
import com.rf.ranking.domain.RankingResult;

public interface RepositoryRankingService {

  RankingResult rank(RankingRequest request);
}

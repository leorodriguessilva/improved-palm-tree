package com.rf.ranking.service;

import com.rf.ranking.domain.RankingRequest;
import com.rf.ranking.domain.RankingResult;
import java.util.Optional;

public interface RankingCache {

  Optional<RankingResult> get(RankingRequest request);

  void put(RankingRequest request, RankingResult result);

}

package com.redcare.pharmacy.ranking.service;

import com.redcare.pharmacy.ranking.domain.RankingRequest;
import com.redcare.pharmacy.ranking.domain.RankingResult;
import java.util.Optional;

public interface RankingCache {

  Optional<RankingResult> get(RankingRequest request);

  void put(RankingRequest request, RankingResult result);

  void clear();
}

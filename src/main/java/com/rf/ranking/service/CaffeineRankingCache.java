package com.rf.ranking.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rf.ranking.domain.RankingRequest;
import com.rf.ranking.domain.RankingResult;
import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CaffeineRankingCache implements RankingCache {

  private final Cache<RankingRequest, RankingResult> cache;

  public CaffeineRankingCache(
      @Value("${cache.ranking.maximum-size:500}") int maxSize,
      @Value("${cache.ranking.ttl:5m}") Duration ttl
  ) {
    this.cache = Caffeine.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(ttl)
        .recordStats()
        .build();
  }

  @Override
  public Optional<RankingResult> get(RankingRequest request) {
    return Optional.ofNullable(cache.getIfPresent(request));
  }

  @Override
  public void put(RankingRequest request, RankingResult result) {
    cache.put(request, result);
  }
}


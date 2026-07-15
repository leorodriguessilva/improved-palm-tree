package com.redcare.pharmacy.ranking.service;

import com.redcare.pharmacy.ranking.domain.RankingRequest;
import com.redcare.pharmacy.ranking.domain.RankingResult;
import com.redcare.pharmacy.ranking.domain.ScoreVersion;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InMemoryRankingCache implements RankingCache {

  private final int maxSize;
  private final long ttlMillis;
  private final Map<String, CacheEntry> cache;

  public InMemoryRankingCache(
      @Value("${cache.ranking.maximum-size:500}") int maxSize,
      @Value("${cache.ranking.ttl:5m}") java.time.Duration ttl
  ) {
    this.maxSize = maxSize;
    this.ttlMillis = ttl.toMillis();
    this.cache = new HashMap<>();
  }

  @Override
  public Optional<RankingResult> get(RankingRequest request) {
    CacheEntry entry = cache.get(buildKey(request));
    if (entry == null) {
      return Optional.empty();
    }

    if (System.currentTimeMillis() - entry.timestamp > ttlMillis) {
      cache.remove(buildKey(request));
      return Optional.empty();
    }

    return Optional.of(entry.result);
  }

  @Override
  public void put(RankingRequest request, RankingResult result) {
    if (cache.size() >= maxSize) {
      cache.clear();
    }
    cache.put(buildKey(request), new CacheEntry(result, System.currentTimeMillis()));
  }

  @Override
  public void clear() {
    cache.clear();
  }

  private String buildKey(RankingRequest request) {
    return request.language()
        + "|" + request.createdAfter()
        + "|" + request.page()
        + "|" + request.limit()
        + "|" + request.scoreVersion().getValue();
  }

  private record CacheEntry(RankingResult result, long timestamp) {}
}

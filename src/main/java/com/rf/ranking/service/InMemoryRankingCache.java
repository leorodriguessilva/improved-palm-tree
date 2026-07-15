package com.rf.ranking.service;

import java.time.Duration;

/**
 * Compatibility wrapper kept for callers that may still instantiate the old class name directly.
 * Spring uses {@link CaffeineRankingCache} as the single cache bean.
 */
@Deprecated(forRemoval = true)
public class InMemoryRankingCache extends CaffeineRankingCache {

  public InMemoryRankingCache(int maxSize, Duration ttl) {
    super(maxSize, ttl);
  }
}

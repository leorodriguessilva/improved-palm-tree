package com.redcare.pharmacy.ranking.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.redcare.pharmacy.ranking.domain.RankingRequest;
import com.redcare.pharmacy.ranking.domain.RankingResult;
import com.redcare.pharmacy.ranking.domain.ScoreVersion;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class CaffeineRankingCacheTest {

  @Test
  void identicalKeysHitCache() {
    var cache = new CaffeineRankingCache(10, Duration.ofMinutes(5));
    var request = request("Java", "2026-01-01", 1, 20);
    var result = result(request);

    cache.put(request, result);

    assertThat(cache.get(request)).containsSame(result);
  }

  @Test
  void differentRequestDimensionsCreateDifferentKeys() {
    var cache = new CaffeineRankingCache(10, Duration.ofMinutes(5));
    var base = request("Java", "2026-01-01", 1, 20);
    cache.put(base, result(base));

    assertThat(cache.get(request("Kotlin", "2026-01-01", 1, 20))).isEmpty();
    assertThat(cache.get(request("Java", "2026-02-01", 1, 20))).isEmpty();
    assertThat(cache.get(request("Java", "2026-01-01", 2, 20))).isEmpty();
    assertThat(cache.get(request("Java", "2026-01-01", 1, 10))).isEmpty();
  }

  @Test
  void expiredEntriesAreAbsent() throws InterruptedException {
    var cache = new CaffeineRankingCache(10, Duration.ofMillis(50));
    var request = request("Java", "2026-01-01", 1, 20);
    cache.put(request, result(request));

    Thread.sleep(100);

    assertThat(cache.get(request)).isEmpty();
  }

  @Test
  void maximumSizeEvictsEntriesWithoutClearingEverything() {
    var cache = new CaffeineRankingCache(2, Duration.ofMinutes(5));
    var first = request("Java", "2026-01-01", 1, 20);
    var second = request("Kotlin", "2026-01-01", 1, 20);
    var third = request("Go", "2026-01-01", 1, 20);

    cache.put(first, result(first));
    cache.put(second, result(second));
    cache.put(third, result(third));

    int present = 0;
    present += cache.get(first).isPresent() ? 1 : 0;
    present += cache.get(second).isPresent() ? 1 : 0;
    present += cache.get(third).isPresent() ? 1 : 0;
    assertThat(present).isGreaterThanOrEqualTo(1);
  }

  @Test
  void concurrentAccessDoesNotCorruptCache() throws InterruptedException {
    var cache = new CaffeineRankingCache(1_000, Duration.ofMinutes(5));
    int threads = 8;
    int operationsPerThread = 100;
    var executor = Executors.newFixedThreadPool(threads);
    var start = new CountDownLatch(1);
    var done = new CountDownLatch(threads);

    for (int thread = 0; thread < threads; thread++) {
      int threadIndex = thread;
      executor.submit(() -> {
        try {
          start.await();
          for (int i = 0; i < operationsPerThread; i++) {
            var request = request("Java" + threadIndex, "2026-01-01", i + 1, 20);
            cache.put(request, result(request));
            cache.get(request);
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        } finally {
          done.countDown();
        }
      });
    }

    start.countDown();

    assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
    executor.shutdownNow();
  }

  private RankingRequest request(String language, String createdAfter, int page, int limit) {
    return new RankingRequest(language, LocalDate.parse(createdAfter), page, limit, ScoreVersion.V1);
  }

  private RankingResult result(RankingRequest request) {
    return new RankingResult(request, ScoreVersion.V1, Instant.parse("2026-07-15T00:00:00Z"), 0, false, List.of());
  }
}


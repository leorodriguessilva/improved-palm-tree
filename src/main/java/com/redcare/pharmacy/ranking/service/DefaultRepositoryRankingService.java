package com.redcare.pharmacy.ranking.service;

import com.redcare.pharmacy.ranking.domain.RankedRepository;
import com.redcare.pharmacy.ranking.domain.RankingRequest;
import com.redcare.pharmacy.ranking.domain.RankingResult;
import com.redcare.pharmacy.ranking.domain.RepositoryCandidate;
import com.redcare.pharmacy.ranking.domain.ScoreBreakdown;
import java.time.Clock;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DefaultRepositoryRankingService implements RepositoryRankingService {

  private final RepositorySearchClient searchClient;
  private final RepositoryScorer scorer;
  private final RankingCache cache;
  private final Clock clock;

  @Autowired
  public DefaultRepositoryRankingService(
      RepositorySearchClient searchClient,
      RepositoryScorer scorer,
      RankingCache cache,
      Clock clock
  ) {
    this.searchClient = searchClient;
    this.scorer = scorer;
    this.cache = cache;
    this.clock = clock;
  }

  public DefaultRepositoryRankingService(
      RepositorySearchClient searchClient,
      RepositoryScorer scorer,
      RankingCache cache
  ) {
    this(searchClient, scorer, cache, Clock.systemUTC());
  }

  @Override
  public RankingResult rank(RankingRequest request) {
    var cached = cache.get(request);
    if (cached.isPresent()) {
      return cached.get();
    }

    var searchResult = searchClient.search(
        request.language(),
        request.createdAfter(),
        request.page(),
        request.limit()
    );

    var rank = new AtomicInteger(1);
    var rankedRepositories = searchResult.repositories().stream()
        .map(candidate -> new ScoredRepository(candidate, scorer.score(candidate, clock)))
        .sorted(getRankingComparator())
        .map(scoredRepository -> RankedRepository.from(
            scoredRepository.candidate(),
            scoredRepository.score(),
            rank.getAndIncrement()
        ))
        .toList();

    var result = new RankingResult(
        request,
        request.scoreVersion(),
        clock.instant(),
        searchResult.totalCount(),
        searchResult.incompleteResults(),
        rankedRepositories
    );

    cache.put(request, result);
    return result;
  }

  private Comparator<ScoredRepository> getRankingComparator() {
    return Comparator
        .comparing(ScoredRepository::score, Comparator.comparingDouble(ScoreBreakdown::total))
        .reversed()
        .thenComparing(scoredRepository -> scoredRepository.candidate().stars(), Comparator.reverseOrder())
        .thenComparing(scoredRepository -> scoredRepository.candidate().forks(), Comparator.reverseOrder())
        .thenComparing(scoredRepository -> scoredRepository.candidate().updatedAt(), Comparator.reverseOrder())
        .thenComparing(scoredRepository -> scoredRepository.candidate().fullName());
  }

  private record ScoredRepository(RepositoryCandidate candidate, ScoreBreakdown score) {}
}

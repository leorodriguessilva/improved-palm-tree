package com.redcare.pharmacy.ranking.service;

import com.redcare.pharmacy.ranking.domain.RankedRepository;
import com.redcare.pharmacy.ranking.domain.RankingRequest;
import com.redcare.pharmacy.ranking.domain.RankingResult;
import com.redcare.pharmacy.ranking.domain.RepositoryCandidate;
import com.redcare.pharmacy.ranking.domain.ScoreBreakdown;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DefaultRepositoryRankingService implements RepositoryRankingService {

  private final RepositorySearchClient searchClient;
  private final RepositoryScorer scorer;
  private final RankingCache cache;
  private final Clock clock;

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

    var scoredRepositories = searchResult.repositories().stream()
        .map(candidate -> {
          ScoreBreakdown score = scorer.score(candidate, clock);
          return RankedRepository.from(candidate, score, 0);
        })
        .sorted(getRankingComparator())
        .toList();

    var rankedRepositories = assignRanks(scoredRepositories);

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

  private Comparator<RankedRepository> getRankingComparator() {
    return Comparator
        .comparing(RankedRepository::score, Comparator.comparingDouble(ScoreBreakdown::total))
        .reversed()
        .thenComparing(RankedRepository::stars, Comparator.reverseOrder())
        .thenComparing(RankedRepository::forks, Comparator.reverseOrder())
        .thenComparing(RankedRepository::updatedAt, Comparator.reverseOrder())
        .thenComparing(RankedRepository::fullName);
  }

  private List<RankedRepository> assignRanks(List<RankedRepository> sorted) {
    return sorted.stream()
        .map((repo) -> RankedRepository.from(
            new RepositoryCandidate(
                repo.id(),
                repo.name(),
                repo.fullName(),
                repo.url(),
                repo.description(),
                repo.language(),
                repo.createdAt(),
                repo.updatedAt(),
                repo.stars(),
                repo.forks()
            ),
            repo.score(),
            sorted.indexOf(repo) + 1
        ))
        .toList();
  }
}

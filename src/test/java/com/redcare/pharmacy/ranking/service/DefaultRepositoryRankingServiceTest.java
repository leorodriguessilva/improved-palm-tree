package com.redcare.pharmacy.ranking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.redcare.pharmacy.ranking.domain.RankedRepository;
import com.redcare.pharmacy.ranking.domain.RankingRequest;
import com.redcare.pharmacy.ranking.domain.RankingResult;
import com.redcare.pharmacy.ranking.domain.RepositoryCandidate;
import com.redcare.pharmacy.ranking.domain.ScoreBreakdown;
import com.redcare.pharmacy.ranking.domain.ScoreVersion;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultRepositoryRankingServiceTest {

  private static final Clock FIXED_CLOCK = Clock.fixed(
	  Instant.parse("2026-07-15T12:00:00Z"),
	  ZoneOffset.UTC
  );

  private FakeSearchClient searchClient;
  private FakeScorer scorer;
  private FakeCache cache;
  private DefaultRepositoryRankingService rankingService;
  private RankingRequest request;

  @BeforeEach
  void setUp() {
	searchClient = new FakeSearchClient();
	scorer = new FakeScorer();
	cache = new FakeCache();
	rankingService = new DefaultRepositoryRankingService(searchClient, scorer, cache, FIXED_CLOCK);
	request = new RankingRequest("Java", LocalDate.parse("2026-01-01"), 2, 10, ScoreVersion.V1);
  }

  @Test
  void rankCallsSearchClientWithRequestParametersAndPreservesMetadata() {
	var repo = candidate(1, "owner/repo", 10, 1, "2026-07-10T00:00:00Z");
	searchClient.nextResult = new RepositorySearchClient.RepositorySearchResult(List.of(repo), 123, true);
	scorer.scores.put("owner/repo", new ScoreBreakdown(42.1234, 42.12, 10.0, 20.0, 12.12));

	RankingResult result = rankingService.rank(request);

	assertThat(searchClient.calls).isEqualTo(1);
	assertThat(searchClient.language).isEqualTo("Java");
	assertThat(searchClient.createdAfter).isEqualTo(LocalDate.parse("2026-01-01"));
	assertThat(searchClient.page).isEqualTo(2);
	assertThat(searchClient.limit).isEqualTo(10);
	assertThat(result.request()).isEqualTo(request);
	assertThat(result.scoreVersion()).isEqualTo(ScoreVersion.V1);
	assertThat(result.generatedAt()).isEqualTo(FIXED_CLOCK.instant());
	assertThat(result.totalCount()).isEqualTo(123);
	assertThat(result.incompleteResults()).isTrue();
	assertThat(result.repositories()).hasSize(1);
	assertThat(cache.putCalls).isEqualTo(1);
  }

  @Test
  void scoresRepositoriesAndRanksByScoreThenStarsForksUpdatedAtAndFullName() {
	var highest = candidate(1, "owner/highest", 1, 1, "2026-01-01T00:00:00Z");
	var starTieWinner = candidate(2, "owner/star-winner", 200, 1, "2026-01-01T00:00:00Z");
	var forkTieWinner = candidate(3, "owner/fork-winner", 100, 20, "2026-01-01T00:00:00Z");
	var updatedTieWinner = candidate(4, "owner/updated-winner", 100, 10, "2026-07-01T00:00:00Z");
	var fullNameTieWinner = candidate(5, "alpha/name-winner", 100, 10, "2026-01-01T00:00:00Z");
	var fullNameTieLoser = candidate(6, "zebra/name-loser", 100, 10, "2026-01-01T00:00:00Z");

	searchClient.nextResult = new RepositorySearchClient.RepositorySearchResult(
		List.of(fullNameTieLoser, updatedTieWinner, forkTieWinner, starTieWinner, highest, fullNameTieWinner),
		6,
		false
	);
	scorer.scores.put("owner/highest", new ScoreBreakdown(51.0, 51.0, 0.0, 0.0, 0.0));
	scorer.scores.put("owner/star-winner", new ScoreBreakdown(50.0, 50.0, 0.0, 0.0, 0.0));
	scorer.scores.put("owner/fork-winner", new ScoreBreakdown(50.0, 50.0, 0.0, 0.0, 0.0));
	scorer.scores.put("owner/updated-winner", new ScoreBreakdown(50.0, 50.0, 0.0, 0.0, 0.0));
	scorer.scores.put("alpha/name-winner", new ScoreBreakdown(50.0, 50.0, 0.0, 0.0, 0.0));
	scorer.scores.put("zebra/name-loser", new ScoreBreakdown(50.0, 50.0, 0.0, 0.0, 0.0));

	RankingResult result = rankingService.rank(request);

	assertThat(scorer.scoredFullNames).containsExactlyInAnyOrder(
		"owner/highest",
		"owner/star-winner",
		"owner/fork-winner",
		"owner/updated-winner",
		"alpha/name-winner",
		"zebra/name-loser"
	);
	assertThat(result.repositories())
		.extracting(RankedRepository::fullName)
		.containsExactly(
			"owner/highest",
			"owner/star-winner",
			"owner/fork-winner",
			"owner/updated-winner",
			"alpha/name-winner",
			"zebra/name-loser"
		);
	assertThat(result.repositories())
		.extracting(RankedRepository::rank)
		.containsExactly(1, 2, 3, 4, 5, 6);
  }

  @Test
  void rawScoreIsUsedBeforeRoundedDisplayScore() {
	var rawWinner = candidate(1, "owner/raw-winner", 1, 1, "2026-01-01T00:00:00Z");
	var rawLoser = candidate(2, "owner/raw-loser", 10_000, 10_000, "2026-07-01T00:00:00Z");
	searchClient.nextResult = new RepositorySearchClient.RepositorySearchResult(List.of(rawLoser, rawWinner), 2, false);
	scorer.scores.put("owner/raw-winner", new ScoreBreakdown(10.004, 10.00, 0.0, 0.0, 0.0));
	scorer.scores.put("owner/raw-loser", new ScoreBreakdown(10.001, 10.00, 0.0, 0.0, 0.0));

	RankingResult result = rankingService.rank(request);

	assertThat(result.repositories())
		.extracting(RankedRepository::fullName)
		.containsExactly("owner/raw-winner", "owner/raw-loser");
	assertThat(result.repositories())
		.extracting(repository -> repository.score().total())
		.containsExactly(10.00, 10.00);
  }

  @Test
  void emptyResultsProduceEmptyRankingAndAreCached() {
	searchClient.nextResult = new RepositorySearchClient.RepositorySearchResult(List.of(), 0, false);

	RankingResult result = rankingService.rank(request);

	assertThat(result.repositories()).isEmpty();
	assertThat(cache.putCalls).isEqualTo(1);
  }

  @Test
  void cacheHitSkipsSearchAndScoring() {
	RankingResult cached = new RankingResult(request, ScoreVersion.V1, FIXED_CLOCK.instant(), 0, false, List.of());
	cache.put(request, cached);

	RankingResult result = rankingService.rank(request);

	assertThat(result).isSameAs(cached);
	assertThat(searchClient.calls).isZero();
	assertThat(scorer.scoredFullNames).isEmpty();
  }

  @Test
  void cacheMissCallsGitHubOnceAndCachesSuccessfulResult() {
	searchClient.nextResult = new RepositorySearchClient.RepositorySearchResult(List.of(), 0, false);

	RankingResult first = rankingService.rank(request);
	RankingResult second = rankingService.rank(request);

	assertThat(first).isSameAs(second);
	assertThat(searchClient.calls).isEqualTo(1);
	assertThat(cache.putCalls).isEqualTo(1);
  }

  @Test
  void failedSearchIsNotCached() {
	searchClient.failure = new IllegalStateException("boom");

	assertThatThrownBy(() -> rankingService.rank(request)).isSameAs(searchClient.failure);

	assertThat(cache.putCalls).isZero();
	assertThat(cache.get(request)).isEmpty();
  }

  private RepositoryCandidate candidate(long id, String fullName, int stars, int forks, String updatedAt) {
	return new RepositoryCandidate(
		id,
		fullName.substring(fullName.indexOf('/') + 1),
		fullName,
		"https://github.com/" + fullName,
		"description",
		"Java",
		Instant.parse("2026-01-01T00:00:00Z"),
		Instant.parse(updatedAt),
		stars,
		forks
	);
  }

  private static final class FakeSearchClient implements RepositorySearchClient {
	private RepositorySearchResult nextResult = new RepositorySearchResult(List.of(), 0, false);
	private RuntimeException failure;
	private int calls;
	private String language;
	private LocalDate createdAfter;
	private int page;
	private int limit;

	@Override
	public RepositorySearchResult search(String language, LocalDate createdAfter, int page, int limit) {
	  calls++;
	  this.language = language;
	  this.createdAfter = createdAfter;
	  this.page = page;
	  this.limit = limit;
	  if (failure != null) {
		throw failure;
	  }
	  return nextResult;
	}
  }

  private static final class FakeScorer implements RepositoryScorer {
	private final Map<String, ScoreBreakdown> scores = new HashMap<>();
	private final List<String> scoredFullNames = new ArrayList<>();

	@Override
	public ScoreBreakdown score(RepositoryCandidate candidate, Clock clock) {
	  assertThat(clock).isSameAs(FIXED_CLOCK);
	  scoredFullNames.add(candidate.fullName());
	  return scores.getOrDefault(candidate.fullName(), new ScoreBreakdown(0.0, 0.0, 0.0, 0.0));
	}
  }

  private static final class FakeCache implements RankingCache {
	private final Map<RankingRequest, RankingResult> entries = new HashMap<>();
	private int putCalls;

	@Override
	public Optional<RankingResult> get(RankingRequest request) {
	  return Optional.ofNullable(entries.get(request));
	}

	@Override
	public void put(RankingRequest request, RankingResult result) {
	  putCalls++;
	  entries.put(request, result);
	}
  }
}


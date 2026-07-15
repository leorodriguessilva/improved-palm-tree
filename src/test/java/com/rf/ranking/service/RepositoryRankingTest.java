package com.rf.ranking.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.rf.ranking.domain.RankedRepository;
import com.rf.ranking.domain.ScoreBreakdown;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;

class RepositoryRankingTest {

  @Test
  void highestScoreIsRankedFirst() {
    var repo1 = createRankedRepository("owner/repo1", 50.0, 100, 10, "2026-07-01T00:00:00Z");
    var repo2 = createRankedRepository("owner/repo2", 75.0, 50, 5, "2026-07-02T00:00:00Z");

    var sorted = List.of(repo1, repo2).stream()
        .sorted(getRankingComparator())
        .toList();

    assertThat(sorted.get(0).score().total()).isEqualTo(75.0);
    assertThat(sorted.get(1).score().total()).isEqualTo(50.0);
  }

  @Test
  void equalScoreUsesStarsDescending() {
    var repo1 = createRankedRepository("owner/repo1", 50.0, 100, 10, "2026-07-01T00:00:00Z");
    var repo2 = createRankedRepository("owner/repo2", 50.0, 200, 5, "2026-07-01T00:00:00Z");

    var sorted = List.of(repo1, repo2).stream()
        .sorted(getRankingComparator())
        .toList();

    assertThat(sorted.get(0).stars()).isEqualTo(200);
    assertThat(sorted.get(1).stars()).isEqualTo(100);
  }

  @Test
  void equalScoreAndStarsUsesForksDescending() {
    var repo1 = createRankedRepository("owner/repo1", 50.0, 100, 10, "2026-07-01T00:00:00Z");
    var repo2 = createRankedRepository("owner/repo2", 50.0, 100, 20, "2026-07-01T00:00:00Z");

    var sorted = List.of(repo1, repo2).stream()
        .sorted(getRankingComparator())
        .toList();

    assertThat(sorted.get(0).forks()).isEqualTo(20);
    assertThat(sorted.get(1).forks()).isEqualTo(10);
  }

  @Test
  void equalScoreStarsAndForksUsesUpdatedDateDescending() {
    var repo1 = createRankedRepository("owner/repo1", 50.0, 100, 10, "2026-07-01T00:00:00Z");
    var repo2 = createRankedRepository("owner/repo2", 50.0, 100, 10, "2026-07-15T00:00:00Z");

    var sorted = List.of(repo1, repo2).stream()
        .sorted(getRankingComparator())
        .toList();

    assertThat(sorted.get(0).updatedAt()).isEqualTo(Instant.parse("2026-07-15T00:00:00Z"));
    assertThat(sorted.get(1).updatedAt()).isEqualTo(Instant.parse("2026-07-01T00:00:00Z"));
  }

  @Test
  void remainingTieUsesFullNameAscending() {
    var repo1 = createRankedRepository("zebra/repo", 50.0, 100, 10, "2026-07-01T00:00:00Z");
    var repo2 = createRankedRepository("alpha/repo", 50.0, 100, 10, "2026-07-01T00:00:00Z");

    var sorted = List.of(repo1, repo2).stream()
        .sorted(getRankingComparator())
        .toList();

    assertThat(sorted.get(0).fullName()).isEqualTo("alpha/repo");
    assertThat(sorted.get(1).fullName()).isEqualTo("zebra/repo");
  }

  @Test
  void rankingStartsAt1() {
    var repo = createRankedRepository("owner/repo", 50.0, 100, 10, "2026-07-01T00:00:00Z");
    List<RankedRepository> sorted = List.of(repo).stream()
        .sorted(getRankingComparator())
        .toList();

    var ranked = assignRanks(sorted);
    assertThat(ranked.get(0).rank()).isEqualTo(1);
  }

  @Test
  void emptyListProducesEmptyResult() {
    List<RankedRepository> sorted = List.<RankedRepository>of().stream()
        .sorted(getRankingComparator())
        .toList();

    var ranked = assignRanks(sorted);
    assertThat(ranked).isEmpty();
  }

  @Test
  void rankingIsStableAcrossRepeatedRuns() {
    var repo1 = createRankedRepository("owner/repo1", 50.0, 100, 10, "2026-07-01T00:00:00Z");
    var repo2 = createRankedRepository("owner/repo2", 50.0, 100, 10, "2026-07-02T00:00:00Z");
    var repo3 = createRankedRepository("owner/repo3", 50.0, 100, 10, "2026-07-03T00:00:00Z");

    var original = List.of(repo1, repo2, repo3);

    var sorted1 = original.stream()
        .sorted(getRankingComparator())
        .toList();

    var sorted2 = original.stream()
        .sorted(getRankingComparator())
        .toList();

    assertThat(sorted1)
        .extracting(RankedRepository::fullName)
        .isEqualTo(
            sorted2.stream()
                .map(RankedRepository::fullName)
                .toList()
        );
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
        .map(repo -> RankedRepository.from(
            new com.rf.ranking.domain.RepositoryCandidate(
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

  private RankedRepository createRankedRepository(
      String fullName,
      double score,
      int stars,
      int forks,
      String updatedAt) {
    return new RankedRepository(
        1L,
        "repo",
        fullName,
        "https://github.com/" + fullName,
        "Test repository",
        "Java",
        Instant.parse("2022-01-01T00:00:00Z"),
        Instant.parse(updatedAt),
        stars,
        forks,
        new ScoreBreakdown(score, 0.0, 0.0, 0.0),
        1
    );
  }
}

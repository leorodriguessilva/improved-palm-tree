package com.redcare.pharmacy.ranking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.redcare.pharmacy.ranking.config.ScoringConfig;
import com.redcare.pharmacy.ranking.domain.RepositoryCandidate;
import com.redcare.pharmacy.ranking.domain.ScoreBreakdown;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RepositoryScorerTest {

  private RepositoryScorer scorer;
  private ScoringConfig config;
  private Clock fixedClock;

  @BeforeEach
  void setUp() {
    config = new ScoringConfig();
    config.validate();
    fixedClock = Clock.fixed(
        Instant.parse("2026-07-15T00:00:00Z"),
        ZoneId.of("UTC")
    );
    scorer = new DefaultRepositoryScorer(config);
  }

  // Star component tests
  @Test
  void zeroStarsProducesZeroStarComponent() {
    var candidate = createCandidate(0, 0);
    var score = scorer.score(candidate, fixedClock);
    assertThat(score.starsContribution()).isCloseTo(0.0, within(0.01));
  }

  @Test
  void moreStarsIncreaseScore() {
    var candidate1 = createCandidate(1000, 0);
    var candidate2 = createCandidate(2000, 0);

    var score1 = scorer.score(candidate1, fixedClock);
    var score2 = scorer.score(candidate2, fixedClock);

    assertThat(score2.total()).isGreaterThan(score1.total());
  }

  @Test
  void referenceMaximumStarsProducesNearMaximum() {
    var candidate = createCandidate(100000, 0);
    var score = scorer.score(candidate, fixedClock);
    assertThat(score.starsContribution()).isCloseTo(60.0, within(0.1));
  }

  @Test
  void valuesAboveReferenceMaximumRemainCapped() {
    var candidate1 = createCandidate(100000, 0);
    var candidate2 = createCandidate(200000, 0);

    var score1 = scorer.score(candidate1, fixedClock);
    var score2 = scorer.score(candidate2, fixedClock);

    assertThat(score2.starsContribution()).isCloseTo(score1.starsContribution(), within(0.01));
  }

  // Fork component tests
  @Test
  void zeroForksProducesZeroForkComponent() {
    var candidate = createCandidate(0, 0);
    var score = scorer.score(candidate, fixedClock);
    assertThat(score.forksContribution()).isCloseTo(0.0, within(0.01));
  }

  @Test
  void moreForksIncreaseScore() {
    var candidate1 = createCandidate(0, 500);
    var candidate2 = createCandidate(0, 1000);

    var score1 = scorer.score(candidate1, fixedClock);
    var score2 = scorer.score(candidate2, fixedClock);

    assertThat(score2.total()).isGreaterThan(score1.total());
  }

  @Test
  void referenceMaximumForksProducesNearMaximum() {
    var candidate = createCandidate(0, 20000);
    var score = scorer.score(candidate, fixedClock);
    assertThat(score.forksContribution()).isCloseTo(25.0, within(0.1));
  }

  @Test
  void valuesAboveReferenceMaximumRemainCappedForForks() {
    var candidate1 = createCandidate(0, 20000);
    var candidate2 = createCandidate(0, 40000);

    var score1 = scorer.score(candidate1, fixedClock);
    var score2 = scorer.score(candidate2, fixedClock);

    assertThat(score2.forksContribution()).isCloseTo(score1.forksContribution(), within(0.01));
  }

  // Recency component tests
  @Test
  void updatedTodayProducesMaximumRecency() {
    var candidate = createCandidate(0, 0, Instant.now(fixedClock));
    var score = scorer.score(candidate, fixedClock);
    assertThat(score.recencyContribution()).isCloseTo(15.0, within(0.1));
  }

  @Test
  void updatedAtHalfLifeProducesApproximatelyHalf() {
    Instant updatedAt = fixedClock.instant().minus(java.time.Duration.ofDays(180));
    var candidate = createCandidate(0, 0, updatedAt);
    var score = scorer.score(candidate, fixedClock);
    assertThat(score.recencyContribution()).isCloseTo(7.5, within(0.5));
  }

  @Test
  void updatedAtTwoHalfLivesProducesApproximatelyQuarter() {
    Instant updatedAt = fixedClock.instant().minus(java.time.Duration.ofDays(360));
    var candidate = createCandidate(0, 0, updatedAt);
    var score = scorer.score(candidate, fixedClock);
    assertThat(score.recencyContribution()).isCloseTo(3.75, within(0.5));
  }

  @Test
  void futureUpdateTimeIsTreatedAsZeroDaysOld() {
    Instant updatedAt = fixedClock.instant().plus(java.time.Duration.ofDays(100));
    var candidate = createCandidate(0, 0, updatedAt);
    var score = scorer.score(candidate, fixedClock);
    assertThat(score.recencyContribution()).isCloseTo(15.0, within(0.1));
  }

  // Final score tests
  @Test
  void finalScoreIsCorrectlyCalculated() {
    var candidate = createCandidate(25000, 4100);
    var score = scorer.score(candidate, fixedClock);

    double expectedTotal = score.starsContribution() + score.forksContribution()
        + score.recencyContribution();
    assertThat(score.total()).isCloseTo(expectedTotal, within(0.01));
  }

  @Test
  void finalScoreRemainsInBounds() {
    var candidate = createCandidate(1000000, 100000);
    var score = scorer.score(candidate, fixedClock);
    assertThat(score.total()).isGreaterThanOrEqualTo(0.0);
    assertThat(score.total()).isLessThanOrEqualTo(100.0);
  }

  @Test
  void sameInputProducesSameScore() {
    var candidate = createCandidate(25000, 4100);
    var score1 = scorer.score(candidate, fixedClock);
    var score2 = scorer.score(candidate, fixedClock);

    assertThat(score1.total()).isEqualTo(score2.total());
    assertThat(score1.starsContribution()).isEqualTo(score2.starsContribution());
    assertThat(score1.forksContribution()).isEqualTo(score2.forksContribution());
    assertThat(score1.recencyContribution()).isEqualTo(score2.recencyContribution());
  }

  @Test
  void increasingStarsNeverDecreasesScore() {
    var candidate1 = createCandidate(1000, 100);
    var candidate2 = createCandidate(2000, 100);

    var score1 = scorer.score(candidate1, fixedClock);
    var score2 = scorer.score(candidate2, fixedClock);

    assertThat(score2.total()).isGreaterThanOrEqualTo(score1.total());
  }

  @Test
  void increasingForksNeverDecreasesScore() {
    var candidate1 = createCandidate(100, 500);
    var candidate2 = createCandidate(100, 1000);

    var score1 = scorer.score(candidate1, fixedClock);
    var score2 = scorer.score(candidate2, fixedClock);

    assertThat(score2.total()).isGreaterThanOrEqualTo(score1.total());
  }

  @Test
  void newerUpdateNeverDecreasesScore() {
    Instant oldUpdate = fixedClock.instant().minus(java.time.Duration.ofDays(365));
    Instant newUpdate = fixedClock.instant().minus(java.time.Duration.ofDays(180));

    var candidate1 = createCandidate(100, 100, oldUpdate);
    var candidate2 = createCandidate(100, 100, newUpdate);

    var score1 = scorer.score(candidate1, fixedClock);
    var score2 = scorer.score(candidate2, fixedClock);

    assertThat(score2.total()).isGreaterThanOrEqualTo(score1.total());
  }

  @Test
  void scoreBreakdownSumsToTotalScore() {
    var candidate = createCandidate(25000, 4100);
    var score = scorer.score(candidate, fixedClock);

    double sum = score.starsContribution() + score.forksContribution()
        + score.recencyContribution();
    assertThat(score.total()).isCloseTo(sum, within(0.01));
  }

  private RepositoryCandidate createCandidate(int stars, int forks) {
    return createCandidate(stars, forks, Instant.now(fixedClock));
  }

  private RepositoryCandidate createCandidate(int stars, int forks, Instant updatedAt) {
    return new RepositoryCandidate(
        1L,
        "test-repo",
        "owner/test-repo",
        "https://github.com/owner/test-repo",
        "Test repository",
        "Java",
        Instant.now(fixedClock),
        updatedAt,
        stars,
        forks
    );
  }
}

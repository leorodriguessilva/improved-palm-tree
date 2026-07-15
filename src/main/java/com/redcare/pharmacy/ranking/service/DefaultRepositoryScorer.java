package com.redcare.pharmacy.ranking.service;

import com.redcare.pharmacy.ranking.config.ScoringConfig;
import com.redcare.pharmacy.ranking.domain.RepositoryCandidate;
import com.redcare.pharmacy.ranking.domain.ScoreBreakdown;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

@Component
public class DefaultRepositoryScorer implements RepositoryScorer {

  private final ScoringConfig config;

  public DefaultRepositoryScorer(ScoringConfig config) {
    this.config = config;
  }

  @Override
  public ScoreBreakdown score(RepositoryCandidate candidate, Clock clock) {
    double starComponent = calculateStarComponent(candidate.stars());
    double forkComponent = calculateForkComponent(candidate.forks());
    double recencyComponent = calculateRecencyComponent(candidate.updatedAt(), clock);

    double starsContribution = starComponent * config.getWeights().getStars() * 100;
    double forksContribution = forkComponent * config.getWeights().getForks() * 100;
    double recencyContribution = recencyComponent * config.getWeights().getRecency() * 100;

    double rawTotal = Math.min(100.0, starsContribution + forksContribution + recencyContribution);
    double total = Math.round(rawTotal * 100.0) / 100.0;

    starsContribution = Math.round(starsContribution * 100.0) / 100.0;
    forksContribution = Math.round(forksContribution * 100.0) / 100.0;
    recencyContribution = Math.round(recencyContribution * 100.0) / 100.0;

    return new ScoreBreakdown(rawTotal, total, starsContribution, forksContribution, recencyContribution);
  }

  private double calculateStarComponent(int stars) {
    int referenceMaximum = config.getStars().getReferenceMaximum();
    int normalizedStars = Math.max(0, stars);
    return Math.min(1.0, Math.log(1.0 + normalizedStars) / Math.log(1.0 + referenceMaximum));
  }

  private double calculateForkComponent(int forks) {
    int referenceMaximum = config.getForks().getReferenceMaximum();
    int normalizedForks = Math.max(0, forks);
    return Math.min(1.0, Math.log(1.0 + normalizedForks) / Math.log(1.0 + referenceMaximum));
  }

  private double calculateRecencyComponent(java.time.Instant updatedAt, Clock clock) {
    long ageDays = Math.max(0, ChronoUnit.DAYS.between(updatedAt, clock.instant()));
    int halfLifeDays = config.getRecency().getHalfLifeDays();
    return Math.exp(-Math.log(2) * ageDays / halfLifeDays);
  }
}

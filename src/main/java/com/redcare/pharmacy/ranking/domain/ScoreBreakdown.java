package com.redcare.pharmacy.ranking.domain;

public record ScoreBreakdown(
    double total,
    double starsContribution,
    double forksContribution,
    double recencyContribution
) {

  public ScoreBreakdown {
    if (total < 0 || total > 100) {
      throw new IllegalArgumentException("total score must be between 0 and 100");
    }
    if (starsContribution < 0 || starsContribution > 100) {
      throw new IllegalArgumentException("starsContribution must be between 0 and 100");
    }
    if (forksContribution < 0 || forksContribution > 100) {
      throw new IllegalArgumentException("forksContribution must be between 0 and 100");
    }
    if (recencyContribution < 0 || recencyContribution > 100) {
      throw new IllegalArgumentException("recencyContribution must be between 0 and 100");
    }
  }
}

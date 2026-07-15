package com.redcare.pharmacy.ranking.domain;

public record ScoreBreakdown(
    double rawTotal,
    double total,
    double starsContribution,
    double forksContribution,
    double recencyContribution
) {

  public ScoreBreakdown(
      double total,
      double starsContribution,
      double forksContribution,
      double recencyContribution
  ) {
    this(total, total, starsContribution, forksContribution, recencyContribution);
  }

  public ScoreBreakdown {
    if (rawTotal < 0 || rawTotal > 100) {
      throw new IllegalArgumentException("rawTotal score must be between 0 and 100");
    }
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

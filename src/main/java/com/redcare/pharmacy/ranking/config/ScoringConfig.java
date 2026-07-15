package com.redcare.pharmacy.ranking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ranking")
public class ScoringConfig {

  private String scoreVersion = "v1";
  private Weights weights = new Weights();
  private Stars stars = new Stars();
  private Forks forks = new Forks();
  private Recency recency = new Recency();

  public void validate() {
    if (weights.stars < 0 || weights.stars > 1) {
      throw new IllegalArgumentException("stars weight must be between 0 and 1");
    }
    if (weights.forks < 0 || weights.forks > 1) {
      throw new IllegalArgumentException("forks weight must be between 0 and 1");
    }
    if (weights.recency < 0 || weights.recency > 1) {
      throw new IllegalArgumentException("recency weight must be between 0 and 1");
    }

    double sum = weights.stars + weights.forks + weights.recency;
    if (Math.abs(sum - 1.0) > 0.0001) {
      throw new IllegalArgumentException(
          "Weights must sum to 1.0, but got: " + sum);
    }

    if (stars.referenceMaximum <= 0) {
      throw new IllegalArgumentException("stars.reference-maximum must be positive");
    }
    if (forks.referenceMaximum <= 0) {
      throw new IllegalArgumentException("forks.reference-maximum must be positive");
    }
    if (recency.halfLifeDays <= 0) {
      throw new IllegalArgumentException("recency.half-life-days must be positive");
    }
  }

  public String getScoreVersion() {
    return scoreVersion;
  }

  public void setScoreVersion(String scoreVersion) {
    this.scoreVersion = scoreVersion;
  }

  public Weights getWeights() {
    return weights;
  }

  public void setWeights(Weights weights) {
    this.weights = weights;
  }

  public Stars getStars() {
    return stars;
  }

  public void setStars(Stars stars) {
    this.stars = stars;
  }

  public Forks getForks() {
    return forks;
  }

  public void setForks(Forks forks) {
    this.forks = forks;
  }

  public Recency getRecency() {
    return recency;
  }

  public void setRecency(Recency recency) {
    this.recency = recency;
  }

  public static class Weights {
    private double stars = 0.60;
    private double forks = 0.25;
    private double recency = 0.15;

    public double getStars() {
      return stars;
    }

    public void setStars(double stars) {
      this.stars = stars;
    }

    public double getForks() {
      return forks;
    }

    public void setForks(double forks) {
      this.forks = forks;
    }

    public double getRecency() {
      return recency;
    }

    public void setRecency(double recency) {
      this.recency = recency;
    }
  }

  public static class Stars {
    private int referenceMaximum = 100000;

    public int getReferenceMaximum() {
      return referenceMaximum;
    }

    public void setReferenceMaximum(int referenceMaximum) {
      this.referenceMaximum = referenceMaximum;
    }
  }

  public static class Forks {
    private int referenceMaximum = 20000;

    public int getReferenceMaximum() {
      return referenceMaximum;
    }

    public void setReferenceMaximum(int referenceMaximum) {
      this.referenceMaximum = referenceMaximum;
    }
  }

  public static class Recency {
    private int halfLifeDays = 180;

    public int getHalfLifeDays() {
      return halfLifeDays;
    }

    public void setHalfLifeDays(int halfLifeDays) {
      this.halfLifeDays = halfLifeDays;
    }
  }
}

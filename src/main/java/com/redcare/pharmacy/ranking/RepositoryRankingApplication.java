package com.redcare.pharmacy.ranking;

import com.redcare.pharmacy.ranking.config.ScoringConfig;
import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class RepositoryRankingApplication {

  private final ScoringConfig scoringConfig;

  public RepositoryRankingApplication(ScoringConfig scoringConfig) {
    this.scoringConfig = scoringConfig;
  }

  public static void main(String[] args) {
    SpringApplication.run(RepositoryRankingApplication.class, args);
  }

  @EventListener(ApplicationReadyEvent.class)
  public void onApplicationReady() {
    scoringConfig.validate();
  }

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}

package com.redcare.pharmacy.ranking.service;

import com.redcare.pharmacy.ranking.domain.RepositoryCandidate;
import com.redcare.pharmacy.ranking.domain.ScoreBreakdown;
import java.time.Clock;

public interface RepositoryScorer {

  ScoreBreakdown score(RepositoryCandidate candidate, Clock clock);
}

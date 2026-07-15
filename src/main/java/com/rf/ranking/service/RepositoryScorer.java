package com.rf.ranking.service;

import com.rf.ranking.domain.RepositoryCandidate;
import com.rf.ranking.domain.ScoreBreakdown;
import java.time.Clock;

public interface RepositoryScorer {

  ScoreBreakdown score(RepositoryCandidate candidate, Clock clock);
}

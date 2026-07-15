package com.redcare.pharmacy.ranking.api;

import com.redcare.pharmacy.ranking.domain.RankingRequest;
import com.redcare.pharmacy.ranking.domain.ScoreVersion;
import com.redcare.pharmacy.ranking.exception.GitHubException;
import com.redcare.pharmacy.ranking.exception.ValidationException;
import com.redcare.pharmacy.ranking.service.RepositoryRankingService;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class RepositoryRankingController {

  private final RepositoryRankingService rankingService;
  private final RequestValidator requestValidator;

  public RepositoryRankingController(
      RepositoryRankingService rankingService,
      RequestValidator requestValidator
  ) {
    this.rankingService = rankingService;
    this.requestValidator = requestValidator;
  }

  @GetMapping("/repositories/rank")
  public ResponseEntity<RankingResponseDto> rankRepositories(
      @RequestParam(required = false) String language,
      @RequestParam(required = false) String createdAfter,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(defaultValue = "v1") String scoreVersion
  ) {
    var correlationId = ErrorResponseDto.generateCorrelationId();

    var validationRequest = new RequestValidator.RankRequest(
        language,
        createdAfter,
        page,
        limit,
        scoreVersion
    );
    requestValidator.validate(validationRequest, correlationId);

    LocalDate createdAfterDate = LocalDate.parse(createdAfter);
    ScoreVersion version = ScoreVersion.fromString(scoreVersion);

    var rankingRequest = new RankingRequest(language, createdAfterDate, page, limit, version);
    var result = rankingService.rank(rankingRequest);

    return ResponseEntity.ok(RankingResponseDto.from(result));
  }

  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<ErrorResponseDto> handleValidationException(ValidationException e) {
    var correlationId = ErrorResponseDto.generateCorrelationId();
    var response = ErrorResponseDto.of(
        400,
        e.getErrorCode(),
        e.getMessage(),
        "/api/v1/repositories/rank",
        correlationId
    );
    return ResponseEntity.status(400).body(response);
  }

  @ExceptionHandler(GitHubException.class)
  public ResponseEntity<ErrorResponseDto> handleGitHubException(GitHubException e) {
    var correlationId = ErrorResponseDto.generateCorrelationId();
    var response = e.getRetryAfter() != null
        ? ErrorResponseDto.of(
            e.getHttpStatus(),
            e.getErrorCode(),
            e.getMessage(),
            "/api/v1/repositories/rank",
            correlationId,
            e.getRetryAfter()
        )
        : ErrorResponseDto.of(
            e.getHttpStatus(),
            e.getErrorCode(),
            e.getMessage(),
            "/api/v1/repositories/rank",
            correlationId
        );
    return ResponseEntity.status(e.getHttpStatus()).body(response);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponseDto> handleException(Exception e) {
    var correlationId = ErrorResponseDto.generateCorrelationId();
    var response = ErrorResponseDto.of(
        500,
        "INTERNAL_ERROR",
        "An unexpected error occurred",
        "/api/v1/repositories/rank",
        correlationId
    );
    return ResponseEntity.status(500).body(response);
  }

  @GetMapping("/health")
  public ResponseEntity<Void> health() {
    return ResponseEntity.ok().build();
  }

  @GetMapping("/health/readiness")
  public ResponseEntity<Void> readiness() {
    return ResponseEntity.ok().build();
  }
}

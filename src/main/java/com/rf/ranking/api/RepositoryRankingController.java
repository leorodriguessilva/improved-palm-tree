package com.rf.ranking.api;

import com.rf.ranking.domain.RankingRequest;
import com.rf.ranking.domain.ScoreVersion;
import com.rf.ranking.exception.GitHubException;
import com.rf.ranking.exception.ValidationException;
import com.rf.ranking.service.RepositoryRankingService;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestController
@RequestMapping("/api/v1")
public class RepositoryRankingController {

  private static final Logger log = LoggerFactory.getLogger(RepositoryRankingController.class);

  private final RepositoryRankingService rankingService;
  private final RequestValidator requestValidator;
  private final String defaultScoreVersion;

  public RepositoryRankingController(
      RepositoryRankingService rankingService,
      RequestValidator requestValidator,
      @Value("${ranking.score-version:v1}") String defaultScoreVersion
  ) {
    this.rankingService = rankingService;
    this.requestValidator = requestValidator;
    this.defaultScoreVersion = defaultScoreVersion;
  }

  @GetMapping("/repositories/rank")
  public ResponseEntity<RankingResponseDto> rankRepositories(
      @RequestParam(required = false) String language,
      @RequestParam(required = false) String createdAfter,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(required = false) String scoreVersion
  ) {
    String requestedScoreVersion = scoreVersion != null ? scoreVersion : defaultScoreVersion;
    var validationRequest = new RequestValidator.RankRequest(
        language,
        createdAfter,
        page,
        limit,
        requestedScoreVersion
    );
    requestValidator.validate(validationRequest);

    LocalDate createdAfterDate = LocalDate.parse(createdAfter);
    ScoreVersion version = ScoreVersion.fromString(requestedScoreVersion);

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

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponseDto> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
    var correlationId = ErrorResponseDto.generateCorrelationId();
    String errorCode = switch (e.getName()) {
      case "page" -> "INVALID_PAGE";
      case "limit" -> "INVALID_LIMIT";
      default -> "INVALID_REQUEST";
    };
    var response = ErrorResponseDto.of(
        400,
        errorCode,
        e.getName() + " has an invalid value",
        "/api/v1/repositories/rank",
        correlationId
    );
    return ResponseEntity.status(400).body(response);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponseDto> handleException(Exception exception) {
    var correlationId = ErrorResponseDto.generateCorrelationId();
    log.error("Unexpected request failure. correlationId={}", correlationId, exception);
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

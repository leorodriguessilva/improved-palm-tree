package com.redcare.pharmacy.ranking.api;

import com.redcare.pharmacy.ranking.exception.ValidationException;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class RequestValidator {

  private static final int MAX_LIMIT = 50;

  public void validate(RankRequest request, String correlationId) {
    validateLanguage(request.language(), correlationId);
    validateCreatedAfter(request.createdAfter(), correlationId);
    validatePage(request.page(), correlationId);
    validateLimit(request.limit(), correlationId);
    validateScoreVersion(request.scoreVersion(), correlationId);
  }

  private void validateLanguage(String language, String correlationId) {
    if (language == null || language.isBlank()) {
      throw new ValidationException(
          "MISSING_LANGUAGE",
          "language parameter is required"
      );
    }
    if (language.contains(":") || language.contains("\"") || language.contains("(")
        || language.contains(")")) {
      throw new ValidationException(
          "INVALID_LANGUAGE",
          "language parameter contains invalid characters"
      );
    }
  }

  private void validateCreatedAfter(String createdAfter, String correlationId) {
    if (createdAfter == null || createdAfter.isBlank()) {
      throw new ValidationException(
          "MISSING_CREATED_AFTER",
          "createdAfter parameter is required"
      );
    }

    try {
      LocalDate.parse(createdAfter);
    } catch (java.time.format.DateTimeParseException e) {
      throw new ValidationException(
          "INVALID_CREATED_AFTER",
          "createdAfter must use ISO date format yyyy-MM-dd"
      );
    }
  }

  private void validatePage(int page, String correlationId) {
    if (page < 1) {
      throw new ValidationException(
          "INVALID_PAGE",
          "page must be >= 1"
      );
    }
  }

  private void validateLimit(int limit, String correlationId) {
    if (limit < 1) {
      throw new ValidationException(
          "INVALID_LIMIT",
          "limit must be >= 1"
      );
    }
    if (limit > MAX_LIMIT) {
      throw new ValidationException(
          "INVALID_LIMIT",
          "limit must be <= " + MAX_LIMIT
      );
    }
  }

  private void validateScoreVersion(String scoreVersion, String correlationId) {
    try {
      com.redcare.pharmacy.ranking.domain.ScoreVersion.fromString(scoreVersion);
    } catch (IllegalArgumentException e) {
      throw new ValidationException(
          "INVALID_SCORE_VERSION",
          "Unsupported score version: " + scoreVersion
      );
    }
  }

  public record RankRequest(
      String language,
      String createdAfter,
      int page,
      int limit,
      String scoreVersion
  ) {}
}

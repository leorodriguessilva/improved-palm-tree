package com.redcare.pharmacy.ranking.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponseDto(
    String type,
    String title,
    int status,
    String detail,
    String instance,
    @JsonProperty("errorCode")
    String errorCode,
    @JsonProperty("retryAfter")
    Integer retryAfter,
    Instant timestamp,
    @JsonProperty("correlationId")
    String correlationId
) {

  public static ErrorResponseDto of(
      int status,
      String errorCode,
      String detail,
      String instance,
      String correlationId
  ) {
    return new ErrorResponseDto(
        "https://example.com/problems/invalid-request",
        mapTitleFromStatus(status),
        status,
        detail,
        instance,
        errorCode,
        null,
        Instant.now(),
        correlationId
    );
  }

  public static ErrorResponseDto of(
      int status,
      String errorCode,
      String detail,
      String instance,
      String correlationId,
      Integer retryAfter
  ) {
    return new ErrorResponseDto(
        "https://example.com/problems/invalid-request",
        mapTitleFromStatus(status),
        status,
        detail,
        instance,
        errorCode,
        retryAfter,
        Instant.now(),
        correlationId
    );
  }

  private static String mapTitleFromStatus(int status) {
    return switch (status) {
      case 400 -> "Invalid request";
      case 429 -> "Rate limit exceeded";
      case 502 -> "Bad gateway";
      case 503 -> "Service unavailable";
      default -> "Internal server error";
    };
  }

  public static String generateCorrelationId() {
    return UUID.randomUUID().toString();
  }
}

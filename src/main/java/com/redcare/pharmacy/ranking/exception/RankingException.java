package com.redcare.pharmacy.ranking.exception;

public abstract class RankingException extends RuntimeException {

  private final String errorCode;

  public RankingException(String errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public RankingException(String errorCode, String message, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }

  public String getErrorCode() {
    return errorCode;
  }
}

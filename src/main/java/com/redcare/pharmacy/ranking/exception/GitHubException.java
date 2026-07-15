package com.redcare.pharmacy.ranking.exception;

public class GitHubException extends RankingException {

  private final int httpStatus;
  private final Integer retryAfter;

  public GitHubException(String errorCode, String message, int httpStatus) {
    super(errorCode, message);
    this.httpStatus = httpStatus;
    this.retryAfter = null;
  }

  public GitHubException(String errorCode, String message, int httpStatus, Integer retryAfter) {
    super(errorCode, message);
    this.httpStatus = httpStatus;
    this.retryAfter = retryAfter;
  }

  public GitHubException(String errorCode, String message, int httpStatus, Throwable cause) {
    super(errorCode, message, cause);
    this.httpStatus = httpStatus;
    this.retryAfter = null;
  }

  public int getHttpStatus() {
    return httpStatus;
  }

  public Integer getRetryAfter() {
    return retryAfter;
  }
}

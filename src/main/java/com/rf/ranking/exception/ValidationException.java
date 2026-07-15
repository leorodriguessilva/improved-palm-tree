package com.rf.ranking.exception;

public class ValidationException extends RankingException {

  public ValidationException(String errorCode, String message) {
    super(errorCode, message);
  }
}

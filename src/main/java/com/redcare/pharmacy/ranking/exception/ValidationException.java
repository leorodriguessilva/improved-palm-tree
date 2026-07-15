package com.redcare.pharmacy.ranking.exception;

public class ValidationException extends RankingException {

  public ValidationException(String errorCode, String message) {
    super(errorCode, message);
  }
}

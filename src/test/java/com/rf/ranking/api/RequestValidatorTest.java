package com.rf.ranking.api;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rf.ranking.exception.ValidationException;
import org.junit.jupiter.api.Test;

class RequestValidatorTest {

  private final RequestValidator validator = new RequestValidator(50);

  @Test
  void validRequestPasses() {
	assertThatCode(() -> validator.validate(request("Java", "2026-01-01", 1, 20, "v1")))
		.doesNotThrowAnyException();
  }

  @Test
  void supportsLanguagesWithPlusAndHashSymbols() {
	assertThatCode(() -> validator.validate(request("C++", "2026-01-01", 1, 20, "v1")))
		.doesNotThrowAnyException();
	assertThatCode(() -> validator.validate(request("C#", "2026-01-01", 1, 20, "v1")))
		.doesNotThrowAnyException();
	assertThatCode(() -> validator.validate(request("F#", "2026-01-01", 1, 20, "v1")))
		.doesNotThrowAnyException();
  }

  @Test
  void missingAndBlankLanguageFail() {
	assertValidationError(request(null, "2026-01-01", 1, 20, "v1"), "MISSING_LANGUAGE");
	assertValidationError(request(" ", "2026-01-01", 1, 20, "v1"), "MISSING_LANGUAGE");
  }

  @Test
  void qualifierInjectionFails() {
	assertValidationError(request("Java stars:>1000", "2026-01-01", 1, 20, "v1"), "INVALID_LANGUAGE");
	assertValidationError(request("Java\"", "2026-01-01", 1, 20, "v1"), "INVALID_LANGUAGE");
	assertValidationError(request("Java(test)", "2026-01-01", 1, 20, "v1"), "INVALID_LANGUAGE");
  }

  @Test
  void missingAndInvalidCreatedAfterFail() {
	assertValidationError(request("Java", null, 1, 20, "v1"), "MISSING_CREATED_AFTER");
	assertValidationError(request("Java", "", 1, 20, "v1"), "MISSING_CREATED_AFTER");
	assertValidationError(request("Java", "01-01-2026", 1, 20, "v1"), "INVALID_CREATED_AFTER");
  }

  @Test
  void invalidPageAndLimitFail() {
	assertValidationError(request("Java", "2026-01-01", 0, 20, "v1"), "INVALID_PAGE");
	assertValidationError(request("Java", "2026-01-01", -1, 20, "v1"), "INVALID_PAGE");
	assertValidationError(request("Java", "2026-01-01", 1, 0, "v1"), "INVALID_LIMIT");
	assertValidationError(request("Java", "2026-01-01", 1, 51, "v1"), "INVALID_LIMIT");
  }

  @Test
  void configuredMaximumPageSizeIsAuthoritative() {
	var smallerLimitValidator = new RequestValidator(25);

	assertThatCode(() -> smallerLimitValidator.validate(request("Java", "2026-01-01", 1, 25, "v1")))
		.doesNotThrowAnyException();
	assertThatThrownBy(() -> smallerLimitValidator.validate(request("Java", "2026-01-01", 1, 26, "v1")))
		.isInstanceOfSatisfying(ValidationException.class,
			exception -> org.assertj.core.api.Assertions.assertThat(exception.getMessage()).isEqualTo("limit must be <= 25"));
  }

  @Test
  void unsupportedScoreVersionFails() {
	assertValidationError(request("Java", "2026-01-01", 1, 20, "v2"), "INVALID_SCORE_VERSION");
  }

  private void assertValidationError(RequestValidator.RankRequest request, String errorCode) {
	assertThatThrownBy(() -> validator.validate(request))
		.isInstanceOfSatisfying(ValidationException.class,
			exception -> org.assertj.core.api.Assertions.assertThat(exception.getErrorCode()).isEqualTo(errorCode));
  }

  private RequestValidator.RankRequest request(
	  String language,
	  String createdAfter,
	  int page,
	  int limit,
	  String scoreVersion
  ) {
	return new RequestValidator.RankRequest(language, createdAfter, page, limit, scoreVersion);
  }
}


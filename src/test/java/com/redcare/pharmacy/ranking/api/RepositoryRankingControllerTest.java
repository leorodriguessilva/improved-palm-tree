package com.redcare.pharmacy.ranking.api;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.redcare.pharmacy.ranking.domain.RankedRepository;
import com.redcare.pharmacy.ranking.domain.RankingRequest;
import com.redcare.pharmacy.ranking.domain.RankingResult;
import com.redcare.pharmacy.ranking.domain.ScoreBreakdown;
import com.redcare.pharmacy.ranking.domain.ScoreVersion;
import com.redcare.pharmacy.ranking.config.ScoringConfig;
import com.redcare.pharmacy.ranking.exception.GitHubException;
import com.redcare.pharmacy.ranking.service.RepositoryRankingService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RepositoryRankingController.class)
@Import(RequestValidator.class)
class RepositoryRankingControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private RepositoryRankingService rankingService;

  @MockBean
  private ScoringConfig scoringConfig;

  @Test
  void validRequestReturns200AndDocumentedSchema() throws Exception {
	when(rankingService.rank(any(RankingRequest.class))).thenReturn(resultWithOneRepository());

	mockMvc.perform(get("/api/v1/repositories/rank")
			.param("language", "Java")
			.param("createdAfter", "2026-01-01")
			.param("page", "1")
			.param("limit", "20")
			.param("scoreVersion", "v1")
			.accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isOk())
		.andExpect(jsonPath("$.query.language").value("Java"))
		.andExpect(jsonPath("$.query.createdAfter").value("2026-01-01"))
		.andExpect(jsonPath("$.query.page").value(1))
		.andExpect(jsonPath("$.query.limit").value(20))
		.andExpect(jsonPath("$.scoreVersion").value("v1"))
		.andExpect(jsonPath("$.generatedAt").value("2026-07-15T12:00:00Z"))
		.andExpect(jsonPath("$.pagination.returnedCount").value(1))
		.andExpect(jsonPath("$.pagination.githubTotalCount").value(1))
		.andExpect(jsonPath("$.pagination.incompleteResults").value(false))
		.andExpect(jsonPath("$.repositories[0].id").value(123))
		.andExpect(jsonPath("$.repositories[0].fullName").value("owner/example"))
		.andExpect(jsonPath("$.repositories[0].score.total").value(74.36))
		.andExpect(jsonPath("$.repositories[0].rank").value(1));
  }

  @Test
  void emptyResultsReturn200() throws Exception {
	var request = new RankingRequest("Java", LocalDate.parse("2026-01-01"), 1, 20, ScoreVersion.V1);
	when(rankingService.rank(any(RankingRequest.class))).thenReturn(
		new RankingResult(request, ScoreVersion.V1, Instant.parse("2026-07-15T12:00:00Z"), 0, false, List.of())
	);

	mockMvc.perform(validRequest())
		.andExpect(status().isOk())
		.andExpect(jsonPath("$.repositories").isArray())
		.andExpect(jsonPath("$.repositories").isEmpty())
		.andExpect(jsonPath("$.pagination.returnedCount").value(0));
  }

  @Test
  void missingLanguageReturns400() throws Exception {
	mockMvc.perform(get("/api/v1/repositories/rank").param("createdAfter", "2026-01-01"))
		.andExpect(status().isBadRequest())
		.andExpect(jsonPath("$.errorCode").value("MISSING_LANGUAGE"))
		.andExpect(jsonPath("$.correlationId", not(blankOrNullString())));
	verifyNoInteractions(rankingService);
  }

  @Test
  void blankLanguageReturns400() throws Exception {
	mockMvc.perform(get("/api/v1/repositories/rank").param("language", " ").param("createdAfter", "2026-01-01"))
		.andExpect(status().isBadRequest())
		.andExpect(jsonPath("$.errorCode").value("MISSING_LANGUAGE"));
  }

  @Test
  void missingCreatedAfterReturns400() throws Exception {
	mockMvc.perform(get("/api/v1/repositories/rank").param("language", "Java"))
		.andExpect(status().isBadRequest())
		.andExpect(jsonPath("$.errorCode").value("MISSING_CREATED_AFTER"));
  }

  @Test
  void invalidDateReturns400() throws Exception {
	mockMvc.perform(get("/api/v1/repositories/rank").param("language", "Java").param("createdAfter", "bad-date"))
		.andExpect(status().isBadRequest())
		.andExpect(jsonPath("$.errorCode").value("INVALID_CREATED_AFTER"));
  }

  @Test
  void invalidPageAndLimitReturn400() throws Exception {
	mockMvc.perform(get("/api/v1/repositories/rank")
			.param("language", "Java").param("createdAfter", "2026-01-01").param("page", "0"))
		.andExpect(status().isBadRequest())
		.andExpect(jsonPath("$.errorCode").value("INVALID_PAGE"));

	mockMvc.perform(get("/api/v1/repositories/rank")
			.param("language", "Java").param("createdAfter", "2026-01-01").param("limit", "51"))
		.andExpect(status().isBadRequest())
		.andExpect(jsonPath("$.errorCode").value("INVALID_LIMIT"));

	mockMvc.perform(get("/api/v1/repositories/rank")
			.param("language", "Java").param("createdAfter", "2026-01-01").param("limit", "abc"))
		.andExpect(status().isBadRequest())
		.andExpect(jsonPath("$.errorCode").value("INVALID_LIMIT"));
  }

  @Test
  void unsupportedScoreVersionAndQualifierInjectionReturn400() throws Exception {
	mockMvc.perform(get("/api/v1/repositories/rank")
			.param("language", "Java").param("createdAfter", "2026-01-01").param("scoreVersion", "v2"))
		.andExpect(status().isBadRequest())
		.andExpect(jsonPath("$.errorCode").value("INVALID_SCORE_VERSION"));

	mockMvc.perform(get("/api/v1/repositories/rank")
			.param("language", "Java stars:>100").param("createdAfter", "2026-01-01"))
		.andExpect(status().isBadRequest())
		.andExpect(jsonPath("$.errorCode").value("INVALID_LANGUAGE"));
  }

  @Test
  void githubRateLimitReturns429WithRetryMetadata() throws Exception {
	doThrow(new GitHubException("GITHUB_RATE_LIMITED", "GitHub API rate limit exceeded", 429, 60))
		.when(rankingService).rank(any(RankingRequest.class));

	mockMvc.perform(validRequest())
		.andExpect(status().isTooManyRequests())
		.andExpect(jsonPath("$.errorCode").value("GITHUB_RATE_LIMITED"))
		.andExpect(jsonPath("$.retryAfter").value(60))
		.andExpect(jsonPath("$.correlationId", not(blankOrNullString())));
  }

  @Test
  void githubTimeoutMalformedResponseAndUnexpectedExceptionMapToDocumentedErrors() throws Exception {
	doThrow(new GitHubException("GITHUB_UNAVAILABLE", "GitHub is temporarily unavailable", 503))
		.when(rankingService).rank(any(RankingRequest.class));
	mockMvc.perform(validRequest()).andExpect(status().isServiceUnavailable())
		.andExpect(jsonPath("$.detail").value("GitHub is temporarily unavailable"));

	reset(rankingService);
	doThrow(new GitHubException("INVALID_GITHUB_RESPONSE", "Failed to parse GitHub response", 502))
		.when(rankingService).rank(any(RankingRequest.class));
	mockMvc.perform(validRequest()).andExpect(status().isBadGateway())
		.andExpect(jsonPath("$.errorCode").value("INVALID_GITHUB_RESPONSE"));

	reset(rankingService);
	doThrow(new IllegalStateException("jdbc:postgresql://internal-host/socket failed"))
		.when(rankingService).rank(any(RankingRequest.class));
	mockMvc.perform(validRequest()).andExpect(status().isInternalServerError())
		.andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
		.andExpect(jsonPath("$.detail").value(not("jdbc:postgresql://internal-host/socket failed")));
  }

  @Test
  void healthEndpointsMatchRuntimePaths() throws Exception {
	mockMvc.perform(get("/api/v1/health"))
		.andExpect(status().isOk())
		.andExpect(header().doesNotExist("Content-Type"));
	mockMvc.perform(get("/api/v1/health/readiness"))
		.andExpect(status().isOk());
  }

  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder validRequest() {
	return get("/api/v1/repositories/rank")
		.param("language", "Java")
		.param("createdAfter", "2026-01-01")
		.param("page", "1")
		.param("limit", "20")
		.param("scoreVersion", "v1")
		.accept(MediaType.APPLICATION_JSON);
  }

  private RankingResult resultWithOneRepository() {
	var request = new RankingRequest("Java", LocalDate.parse("2026-01-01"), 1, 20, ScoreVersion.V1);
	var repository = new RankedRepository(
		123L,
		"example",
		"owner/example",
		"https://github.com/owner/example",
		"Example repository",
		"Java",
		Instant.parse("2022-03-01T10:00:00Z"),
		Instant.parse("2026-07-14T10:00:00Z"),
		25_000,
		4_100,
		new ScoreBreakdown(74.364, 74.36, 45.12, 17.31, 11.93),
		1
	);
	return new RankingResult(request, ScoreVersion.V1, Instant.parse("2026-07-15T12:00:00Z"), 1, false, List.of(repository));
  }
}




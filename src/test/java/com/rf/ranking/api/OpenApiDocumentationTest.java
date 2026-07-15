package com.rf.ranking.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenApiDocumentationTest {

  @Autowired
  private TestRestTemplate restTemplate;

  @Test
  void apiDocsAreAvailableAtConfiguredPath() {
    var response = restTemplate.getForEntity("/api-docs", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody())
        .contains("\"openapi\"")
        .contains("\"paths\"")
        .contains("/api/v1/repositories/rank")
        .contains("/api/v1/health")
        .contains("/api/v1/health/readiness")
        .contains("scoreVersion")
        .contains("githubTotalCount");
  }

  @Test
  void staticOpenApiDocumentsRuntimePathsAndErrorSchema() throws Exception {
    String openApi = Files.readString(Path.of("src/main/resources/openapi.yaml"));

    assertThat(openApi)
        .contains("/api/v1/repositories/rank")
        .contains("/api/v1/health")
        .contains("/api/v1/health/readiness")
        .contains("correlationId")
        .contains("GITHUB_RATE_LIMITED")
        .contains("Rank within the GitHub candidate page");
  }
}


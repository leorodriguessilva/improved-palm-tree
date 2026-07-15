package com.rf.ranking.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenApiDocumentationTest {

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private ObjectMapper objectMapper;

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
  void runtimeApiPathsMatchStaticOpenApiContract() throws Exception {
    var response = restTemplate.getForEntity("/api-docs", String.class);
    String staticOpenApi = Files.readString(Path.of("src/main/resources/openapi.yaml"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(runtimeApiPaths(response.getBody())).containsExactlyElementsOf(staticApiPaths(staticOpenApi));
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

  private Set<String> runtimeApiPaths(String apiDocsJson) throws Exception {
    Iterator<String> pathNames = objectMapper.readTree(apiDocsJson).path("paths").fieldNames();
    Set<String> paths = new TreeSet<>();
    pathNames.forEachRemaining(path -> {
      if (path.startsWith("/api/v1")) {
        paths.add(path);
      }
    });
    return paths;
  }

  private Set<String> staticApiPaths(String openApiYaml) {
    Set<String> paths = new TreeSet<>();
    openApiYaml.lines()
        .map(String::trim)
        .filter(line -> line.startsWith("/api/v1") && line.endsWith(":"))
        .map(line -> line.substring(0, line.length() - 1))
        .forEach(paths::add);
    return paths;
  }
}


package com.redcare.pharmacy.ranking.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;

class GitHubClientConfigTest {

  @Test
  void githubRestTemplateAppliesConfiguredTimeouts() {
    var restTemplate = new GitHubClientConfig().githubRestTemplate(
        Duration.ofSeconds(2),
        Duration.ofSeconds(5)
    );

    assertThat(restTemplate.getRequestFactory()).isInstanceOf(SimpleClientHttpRequestFactory.class);
    var requestFactory = (SimpleClientHttpRequestFactory) restTemplate.getRequestFactory();

    assertThat(ReflectionTestUtils.getField(requestFactory, "connectTimeout")).isEqualTo(2000);
    assertThat(ReflectionTestUtils.getField(requestFactory, "readTimeout")).isEqualTo(5000);
  }
}


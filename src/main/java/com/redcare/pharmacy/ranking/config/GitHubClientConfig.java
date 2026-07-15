package com.redcare.pharmacy.ranking.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class GitHubClientConfig {

  @Bean
  public RestTemplate githubRestTemplate(
      @Value("${github.connect-timeout:2s}") Duration connectTimeout,
      @Value("${github.response-timeout:5s}") Duration responseTimeout
  ) {
    var requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(toTimeoutMillis(connectTimeout));
    requestFactory.setReadTimeout(toTimeoutMillis(responseTimeout));
    return new RestTemplate(requestFactory);
  }

  private int toTimeoutMillis(Duration timeout) {
    return Math.toIntExact(timeout.toMillis());
  }
}


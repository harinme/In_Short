package com.inshort.be.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  OpenAPI inShortOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("In Short API")
                .description("In Short backend HTTP API specification")
                .version("v1"));
  }
}

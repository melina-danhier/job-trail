package com.melina.jobtrail.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI jobTrailOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("JobTrail API")
                        .version("1.0")
                        .description("REST API for tracking job applications and optional AI-assisted job matching."))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .components(new Components().addSecuritySchemes(BEARER_AUTH,
                        new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}

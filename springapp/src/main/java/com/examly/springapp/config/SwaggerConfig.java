package com.examly.springapp.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI setup (springdoc). Open http://localhost:8080/swagger-ui.html, click "Authorize" and paste
 * the access token from POST /api/login (without the word "Bearer") to try the secured endpoints.
 *
 * @author Team Lead
 */
@Configuration
public class SwaggerConfig {
    /** Name of the security scheme; also used to apply it to every endpoint. */
    public static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI eLearningOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("E-Learning Platform API")
                        .description("REST API of the E-Learning Platform: authentication, courses, customers, "
                                + "cart, orders, reviews, dashboards and AI course search. "
                                + "Secured endpoints need a JWT access token (Authorize button).")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("E-Learning Platform Team")
                                .email("elearning-team@example.com")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the access token returned by POST /api/login")))
                // applied globally: every operation shows the lock icon
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}

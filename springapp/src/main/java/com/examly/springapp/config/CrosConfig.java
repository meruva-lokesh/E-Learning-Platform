package com.examly.springapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS rules: which browser origins may call the API.
 *
 * @author Suriya
 */
@Configuration
public class CrosConfig implements WebMvcConfigurer {
    private final String[] allowedOrigins;

    public CrosConfig(@Value("${app.cors.allowed-origins}") String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "X-Admin-Code", "X-Request-Id")
                .exposedHeaders("Retry-After", "X-Request-Id")
                .allowCredentials(true)
                .maxAge(3600);
    }
}

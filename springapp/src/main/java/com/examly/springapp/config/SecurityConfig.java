package com.examly.springapp.config;

import com.examly.springapp.repository.UserRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/**
 * Who may call which endpoint. Two filter chains exist:
 * <ol>
 * <li>Swagger/OpenAPI documentation paths (public, with a relaxed
 * Content-Security-Policy so the UI can load)</li>
 * <li>The API itself (JWT, roles, strict "default-src 'none'" policy)</li>
 * </ol>
 * In the API chain the ORDER of the requestMatchers matters: the first match
 * wins, so specific rules come first.
 *
 * @author Suriya
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    /** Paths served by springdoc: the OpenAPI JSON/YAML and the Swagger UI. */
    private static final String[] SWAGGER_PATHS = {
            "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml"
    };

    /**
     * Swagger UI needs its own scripts, styles and inline styles; this is only used
     * for the documentation paths.
     */
    private static final String SWAGGER_CSP = "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; "
            + "img-src 'self' data:; font-src 'self' data:; frame-ancestors 'none'";

    private final JwtUtil jwtUtil;
    private final UserRepo userRepo;
    private final RateLimiter rateLimiter;
    private final int globalPerMinute;
    private final int authPerMinute;

    public SecurityConfig(JwtUtil jwtUtil, UserRepo userRepo, RateLimiter rateLimiter,
            @Value("${ratelimit.global.per-minute:120}") int globalPerMinute,
            @Value("${ratelimit.auth.per-minute:10}") int authPerMinute) {
        this.jwtUtil = jwtUtil;
        this.userRepo = userRepo;
        this.rateLimiter = rateLimiter;
        this.globalPerMinute = globalPerMinute;
        this.authPerMinute = authPerMinute;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Chain for the API documentation. It is matched first (Order 1) and only for
     * the Swagger paths, permits everyone and relaxes the Content-Security-Policy
     * for those paths only. The request-id and rate-limit filters still run, so the
     * docs cannot be used to flood the server.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain swaggerFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(SWAGGER_PATHS)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(h -> h
                        .contentSecurityPolicy(c -> c.policyDirectives(SWAGGER_CSP))
                        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        http.addFilterBefore(new RateLimitFilter(rateLimiter, globalPerMinute, authPerMinute),
                UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(new RequestIdFilter(), RateLimitFilter.class);
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        RestSecurityHandlers handlers = new RestSecurityHandlers();
        http
                // stateless API: no cookies authenticate API calls, only the Bearer token
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(h -> h
                        .contentSecurityPolicy(c -> c.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)))
                .exceptionHandling(e -> e.authenticationEntryPoint(handlers).accessDeniedHandler(handlers))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/register", "/api/login", "/api/refresh", "/api/logout", "/api/health")
                        .permitAll()

                        // API documentation (normally served by the swagger chain above; listed here
                        // too so it is never blocked)
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs", "/v3/api-docs/**",
                                "/v3/api-docs.yaml")
                        .permitAll()

                        // admin management: only an existing admin can list, add or remove admins
                        .requestMatchers("/api/admin/**").hasAuthority("ADMIN")

                        // AI search: logged-in customers only (usage is limited per user)
                        .requestMatchers("/api/course-ai/search").hasAuthority("CUSTOMER")

                        // courses ("/api/course/search" is listed first for clarity; the general GET
                        // rule below also covers it)
                        .requestMatchers(HttpMethod.GET, "/api/course/search").hasAnyAuthority("ADMIN", "CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/course", "/api/course/**")
                        .hasAnyAuthority("ADMIN", "CUSTOMER")
                        .requestMatchers("/api/course", "/api/course/**").hasAuthority("ADMIN")

                        // customers (ownership is checked again inside the controller)
                        .requestMatchers(HttpMethod.POST, "/api/customer").hasAuthority("CUSTOMER")
                        .requestMatchers(HttpMethod.PUT, "/api/customer/**").hasAuthority("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/customer/user/**").hasAuthority("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/customer/**").hasAuthority("ADMIN")

                        // orders
                        .requestMatchers(HttpMethod.POST, "/api/order").hasAuthority("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/order").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/order/customer/**").hasAuthority("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/order/**").hasAnyAuthority("ADMIN", "CUSTOMER")
                        .requestMatchers(HttpMethod.DELETE, "/api/order/**").hasAuthority("CUSTOMER")

                        // reviews
                        .requestMatchers(HttpMethod.POST, "/api/review").hasAuthority("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/review/user/**").hasAuthority("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/review/summary", "/api/review/course/**")
                        .hasAnyAuthority("ADMIN", "CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/review", "/api/review/**")
                        .hasAnyAuthority("ADMIN", "CUSTOMER")
                        .requestMatchers(HttpMethod.DELETE, "/api/review/**").hasAuthority("ADMIN")

                        // dashboards: customers see their own numbers (checked again in the
                        // controller), admins see the platform
                        .requestMatchers(HttpMethod.GET, "/api/dashboard/admin").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/dashboard/customer/**").hasAuthority("CUSTOMER")

                        // carts
                        .requestMatchers(HttpMethod.GET, "/api/cart").hasAuthority("ADMIN")
                        .requestMatchers("/api/cart", "/api/cart/**").hasAuthority("CUSTOMER")

                        .anyRequest().authenticated());

        // filter order: request id -> rate limit -> JWT
        http.addFilterBefore(new RateLimitFilter(rateLimiter, globalPerMinute, authPerMinute),
                UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(new RequestIdFilter(), RateLimitFilter.class);
        http.addFilterAfter(new JwtRequestFilter(jwtUtil, userRepo), RateLimitFilter.class);
        return http.build();
    }
}
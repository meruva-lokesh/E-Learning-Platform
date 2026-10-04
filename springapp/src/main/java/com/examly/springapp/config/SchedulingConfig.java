package com.examly.springapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on @Scheduled jobs (rate-limiter purge, refresh-token cleanup).
 *
 * @author Suriya
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}

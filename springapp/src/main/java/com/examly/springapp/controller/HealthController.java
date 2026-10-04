package com.examly.springapp.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Liveness probe used by deployments and by the README ("is the backend up?").
 *
 * @author Team Lead
 */
@RestController
@Tag(name = "Health", description = "Is the backend running?")
public class HealthController {
    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    @Operation(summary = "Health check", description = "Public.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Backend is up")
    })
    @GetMapping("/api/health")
    public ResponseEntity<Map<String, String>> health() {
        log.trace("health called");
        return ResponseEntity.status(HttpStatus.OK).body(Map.of("status", "UP"));
    }
}

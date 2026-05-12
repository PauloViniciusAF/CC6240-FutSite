package com.futsite.controller;

import com.futsite.dto.response.DatabaseStatusResponse;
import com.futsite.service.DatabaseStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/database")
@RequiredArgsConstructor
public class DatabaseController {

    private final DatabaseStatusService databaseStatusService;


    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        log.debug("Database status request received");

        try {
            DatabaseStatusResponse status = databaseStatusService.getDatabaseStatus();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Error getting database status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving database status: " + e.getMessage()));
        }
    }

    @GetMapping("/status/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        log.debug("Health check requested");
        return ResponseEntity.ok("Backend is running");
    }
}

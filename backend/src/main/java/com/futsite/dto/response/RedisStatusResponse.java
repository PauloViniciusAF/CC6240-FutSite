package com.futsite.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedisStatusResponse {
    private String status; // "ACTIVE" or "INACTIVE"
    private String connection;
    private Long responseTimeMs;
    private String message;
    private String uptime; // e.g., "2 days"
    private String checkCommand; // e.g., "redis-cli ping"
}

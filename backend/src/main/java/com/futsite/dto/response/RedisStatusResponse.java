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
    private String status; // "UP" or "DOWN"
    private String connection;
    private Long responseTimeMs;
    private String message;
}

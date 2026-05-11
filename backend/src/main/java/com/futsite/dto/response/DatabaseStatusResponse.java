package com.futsite.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseStatusResponse {
    private String timestamp;
    private PostgresStatusResponse postgres;
    private MongoStatusResponse mongo;
    private RedisStatusResponse redis;
    private String overallStatus; // "HEALTHY", "DEGRADED", "DOWN"
}

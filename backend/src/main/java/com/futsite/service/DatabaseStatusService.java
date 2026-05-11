package com.futsite.service;

import com.futsite.dto.response.DatabaseStatusResponse;
import com.futsite.dto.response.MongoStatusResponse;
import com.futsite.dto.response.PostgresStatusResponse;
import com.futsite.dto.response.RedisStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseStatusService {

    private final DataSource dataSource;
    private final Optional<MongoTemplate> mongoTemplate;
    private final Optional<StringRedisTemplate> redisTemplate;

    public DatabaseStatusResponse getDatabaseStatus() {
        try {
            PostgresStatusResponse postgresStatus = checkPostgresStatus();
            MongoStatusResponse mongoStatus = checkMongoStatus();
            RedisStatusResponse redisStatus = checkRedisStatus();

            String overallStatus = determineOverallStatus(postgresStatus, mongoStatus, redisStatus);

            return DatabaseStatusResponse.builder()
                    .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .postgres(postgresStatus)
                    .mongo(mongoStatus)
                    .redis(redisStatus)
                    .overallStatus(overallStatus)
                    .build();
        } catch (Exception e) {
            log.error("Error in getDatabaseStatus", e);
            return DatabaseStatusResponse.builder()
                    .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .postgres(PostgresStatusResponse.builder().status("DOWN").message("Error: " + e.getMessage()).responseTimeMs(0L).build())
                    .mongo(MongoStatusResponse.builder().status("DOWN").message("Error: " + e.getMessage()).responseTimeMs(0L).build())
                    .redis(RedisStatusResponse.builder().status("DOWN").message("Error: " + e.getMessage()).responseTimeMs(0L).build())
                    .overallStatus("DOWN")
                    .build();
        }
    }

    private PostgresStatusResponse checkPostgresStatus() {
        long startTime = System.currentTimeMillis();
        try {
            Connection connection = dataSource.getConnection();
            boolean isValid = connection.isValid(5);
            long responseTime = System.currentTimeMillis() - startTime;
            
            if (isValid) {
                String database = connection.getCatalog();
                String connString = connection.getMetaData().getURL();
                connection.close();
                
                return PostgresStatusResponse.builder()
                        .status("UP")
                        .connection(connString)
                        .database(database)
                        .connectionPoolSize(getPostgresPoolSize())
                        .responseTimeMs(responseTime)
                        .message("PostgreSQL is running and healthy")
                        .build();
            } else {
                connection.close();
                return PostgresStatusResponse.builder()
                        .status("DOWN")
                        .responseTimeMs(responseTime)
                        .message("PostgreSQL connection validation failed")
                        .build();
            }
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            log.error("PostgreSQL check error", e);
            return PostgresStatusResponse.builder()
                    .status("DOWN")
                    .responseTimeMs(responseTime)
                    .message("PostgreSQL error: " + e.getMessage())
                    .build();
        }
    }

    private MongoStatusResponse checkMongoStatus() {
        long startTime = System.currentTimeMillis();
        try {
            if (!mongoTemplate.isPresent()) {
                long responseTime = System.currentTimeMillis() - startTime;
                return MongoStatusResponse.builder()
                        .status("DOWN")
                        .responseTimeMs(responseTime)
                        .message("MongoDB template not configured")
                        .build();
            }
            
            mongoTemplate.get().executeCommand("{ ping: 1 }");
            long responseTime = System.currentTimeMillis() - startTime;
            
            return MongoStatusResponse.builder()
                    .status("UP")
                    .connection("mongodb://localhost:27017")
                    .database("futsite")
                    .responseTimeMs(responseTime)
                    .message("MongoDB is running and healthy")
                    .build();
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            log.error("MongoDB check error", e);
            return MongoStatusResponse.builder()
                    .status("DOWN")
                    .responseTimeMs(responseTime)
                    .message("MongoDB error: " + e.getMessage())
                    .build();
        }
    }

    private RedisStatusResponse checkRedisStatus() {
        long startTime = System.currentTimeMillis();
        try {
            if (!redisTemplate.isPresent()) {
                long responseTime = System.currentTimeMillis() - startTime;
                return RedisStatusResponse.builder()
                        .status("DOWN")
                        .responseTimeMs(responseTime)
                        .message("Redis template not configured")
                        .build();
            }
            
            String pong = redisTemplate.get().getConnectionFactory()
                    .getConnection()
                    .ping();
            long responseTime = System.currentTimeMillis() - startTime;
            
            if ("PONG".equalsIgnoreCase(pong)) {
                return RedisStatusResponse.builder()
                        .status("UP")
                        .connection("redis://localhost:6379")
                        .responseTimeMs(responseTime)
                        .message("Redis is running and healthy")
                        .build();
            } else {
                return RedisStatusResponse.builder()
                        .status("DOWN")
                        .responseTimeMs(responseTime)
                        .message("Redis ping returned unexpected response")
                        .build();
            }
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            log.error("Redis check error", e);
            return RedisStatusResponse.builder()
                    .status("DOWN")
                    .responseTimeMs(responseTime)
                    .message("Redis error: " + e.getMessage())
                    .build();
        }
    }

    private Long getPostgresPoolSize() {
        try {
            if (dataSource.getClass().getName().contains("HikariDataSource")) {
                return 10L; // Default HikariCP pool size
            }
        } catch (Exception e) {
            log.debug("Could not get pool size", e);
        }
        return null;
    }

    private String determineOverallStatus(PostgresStatusResponse postgres, 
                                         MongoStatusResponse mongo, 
                                         RedisStatusResponse redis) {
        boolean postgresUp = "UP".equals(postgres.getStatus());
        boolean mongoUp = "UP".equals(mongo.getStatus());
        boolean redisUp = "UP".equals(redis.getStatus());

        if (postgresUp && mongoUp && redisUp) {
            return "HEALTHY";
        } else if (postgresUp || mongoUp || redisUp) {
            return "DEGRADED";
        } else {
            return "DOWN";
        }
    }
}

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
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseStatusService {

    private final DataSource dataSource;
    private final Optional<MongoTemplate> mongoTemplate;
    private final Optional<StringRedisTemplate> redisTemplate;

    private static final String POSTGRES_CONTAINER = "postgres";
    private static final String MONGO_CONTAINER = "mongo";
    private static final String REDIS_CONTAINER = "redis";

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
                    .postgres(PostgresStatusResponse.builder().status("INACTIVE").message("Error: " + e.getMessage()).responseTimeMs(0L).checkCommand("pg_isready -h localhost -p 5432").build())
                    .mongo(MongoStatusResponse.builder().status("INACTIVE").message("Error: " + e.getMessage()).responseTimeMs(0L).checkCommand("nc -zvv localhost 27017").build())
                    .redis(RedisStatusResponse.builder().status("INACTIVE").message("Error: " + e.getMessage()).responseTimeMs(0L).checkCommand("redis-cli ping").build())
                    .overallStatus("DOWN")
                    .build();
        }
    }

    private PostgresStatusResponse checkPostgresStatus() {
        long startTime = System.currentTimeMillis();
        try {
            // Try connection first
            Connection connection = dataSource.getConnection();
            boolean isValid = connection.isValid(5);
            long responseTime = System.currentTimeMillis() - startTime;
            
            if (isValid) {
                String database = connection.getCatalog();
                String connString = connection.getMetaData().getURL();
                connection.close();
                
                String uptime = getContainerUptime(POSTGRES_CONTAINER);
                
                return PostgresStatusResponse.builder()
                        .status("ACTIVE")
                        .connection(connString)
                        .database(database)
                        .connectionPoolSize(getPostgresPoolSize())
                        .responseTimeMs(responseTime)
                        .uptime(uptime)
                        .checkCommand("pg_isready -h localhost -p 5432")
                        .message("PostgreSQL is running and healthy")
                        .build();
            } else {
                connection.close();
                String uptime = getContainerUptime(POSTGRES_CONTAINER);
                return PostgresStatusResponse.builder()
                        .status("INACTIVE")
                        .responseTimeMs(responseTime)
                        .uptime(uptime)
                        .checkCommand("pg_isready -h localhost -p 5432")
                        .message("PostgreSQL connection validation failed")
                        .build();
            }
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            log.error("PostgreSQL check error", e);
            String uptime = getContainerUptime(POSTGRES_CONTAINER);
            return PostgresStatusResponse.builder()
                    .status("INACTIVE")
                    .responseTimeMs(responseTime)
                    .uptime(uptime)
                    .checkCommand("pg_isready -h localhost -p 5432")
                    .message("PostgreSQL error: " + e.getMessage())
                    .build();
        }
    }

    private MongoStatusResponse checkMongoStatus() {
        long startTime = System.currentTimeMillis();
        try {
            if (!mongoTemplate.isPresent()) {
                long responseTime = System.currentTimeMillis() - startTime;
                String uptime = getContainerUptime(MONGO_CONTAINER);
                return MongoStatusResponse.builder()
                        .status("INACTIVE")
                        .responseTimeMs(responseTime)
                        .uptime(uptime)
                        .checkCommand("nc -zvv localhost 27017")
                        .message("MongoDB template not configured")
                        .build();
            }
            
            mongoTemplate.get().executeCommand("{ ping: 1 }");
            long responseTime = System.currentTimeMillis() - startTime;
            String uptime = getContainerUptime(MONGO_CONTAINER);
            
            return MongoStatusResponse.builder()
                    .status("ACTIVE")
                    .connection("mongodb://localhost:27017")
                    .database("futsite")
                    .responseTimeMs(responseTime)
                    .uptime(uptime)
                    .checkCommand("nc -zvv localhost 27017")
                    .message("MongoDB is running and healthy")
                    .build();
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            log.error("MongoDB check error", e);
            String uptime = getContainerUptime(MONGO_CONTAINER);
            return MongoStatusResponse.builder()
                    .status("INACTIVE")
                    .responseTimeMs(responseTime)
                    .uptime(uptime)
                    .checkCommand("nc -zvv localhost 27017")
                    .message("MongoDB error: " + e.getMessage())
                    .build();
        }
    }

    private RedisStatusResponse checkRedisStatus() {
        long startTime = System.currentTimeMillis();
        try {
            if (!redisTemplate.isPresent()) {
                long responseTime = System.currentTimeMillis() - startTime;
                String uptime = getContainerUptime(REDIS_CONTAINER);
                return RedisStatusResponse.builder()
                        .status("INACTIVE")
                        .responseTimeMs(responseTime)
                        .uptime(uptime)
                        .checkCommand("redis-cli ping")
                        .message("Redis template not configured")
                        .build();
            }
            
            String pong = redisTemplate.get().getConnectionFactory()
                    .getConnection()
                    .ping();
            long responseTime = System.currentTimeMillis() - startTime;
            String uptime = getContainerUptime(REDIS_CONTAINER);
            
            if ("PONG".equalsIgnoreCase(pong)) {
                return RedisStatusResponse.builder()
                        .status("ACTIVE")
                        .connection("redis://localhost:6379")
                        .responseTimeMs(responseTime)
                        .uptime(uptime)
                        .checkCommand("redis-cli ping")
                        .message("Redis is running and healthy")
                        .build();
            } else {
                return RedisStatusResponse.builder()
                        .status("INACTIVE")
                        .responseTimeMs(responseTime)
                        .uptime(uptime)
                        .checkCommand("redis-cli ping")
                        .message("Redis ping returned unexpected response")
                        .build();
            }
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            log.error("Redis check error", e);
            String uptime = getContainerUptime(REDIS_CONTAINER);
            return RedisStatusResponse.builder()
                    .status("INACTIVE")
                    .responseTimeMs(responseTime)
                    .uptime(uptime)
                    .checkCommand("redis-cli ping")
                    .message("Redis error: " + e.getMessage())
                    .build();
        }
    }

    private String getContainerUptime(String containerName) {
        try {
            ProcessBuilder pb = new ProcessBuilder("docker", "ps", 
                    "--format", "table {{.Names}}\\t{{.Status}}", 
                    "--filter", "name=" + containerName);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            boolean completed = process.waitFor(5, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                return "Unknown";
            }
            
            String output = new String(process.getInputStream().readAllBytes());
            String[] lines = output.split("\n");
            
            // Skip header line and find container
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.contains(containerName)) {
                    String[] parts = line.split("\\s+", 2);
                    if (parts.length > 1) {
                        String status = parts[1];
                        // Extract uptime from status like "Up 2 days" or "Up 3 hours"
                        return status;
                    }
                }
            }
            return "Not running";
        } catch (Exception e) {
            log.debug("Error getting container uptime for {}: {}", containerName, e.getMessage());
            return "Unknown";
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
        boolean postgresActive = "ACTIVE".equals(postgres.getStatus());
        boolean mongoActive = "ACTIVE".equals(mongo.getStatus());
        boolean redisActive = "ACTIVE".equals(redis.getStatus());

        if (postgresActive && mongoActive && redisActive) {
            return "HEALTHY";
        } else if (postgresActive || mongoActive || redisActive) {
            return "DEGRADED";
        } else {
            return "DOWN";
        }
    }
}

package com.futsite.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Configuration that allows the application to start even if databases are unavailable.
 * Makes database connections lazy and non-blocking during startup.
 */
@Slf4j
@Configuration
public class LazyDataSourceConfiguration {

    @Component
    public static class DatabaseHealthCheck {
        private final DataSource dataSource;
        private volatile boolean databaseReady = false;

        public DatabaseHealthCheck(DataSource dataSource) {
            this.dataSource = dataSource;
        }

        @EventListener
        public void onApplicationEvent(ContextRefreshedEvent event) {
            new Thread(() -> {
                try {
                    Thread.sleep(2000); // Wait 2 seconds before trying
                    testConnection();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "DatabaseHealthCheckThread").start();
        }

        private void testConnection() {
            int maxRetries = 3;
            int retryCount = 0;
            
            while (retryCount < maxRetries && !databaseReady) {
                try {
                    var connection = dataSource.getConnection();
                    connection.close();
                    databaseReady = true;
                    log.info("✓ PostgreSQL database is available");
                } catch (DataAccessException | java.sql.SQLException e) {
                    retryCount++;
                    if (retryCount < maxRetries) {
                        log.debug("PostgreSQL not available yet, retrying... ({}/ {})", retryCount, maxRetries);
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    } else {
                        log.warn("✗ PostgreSQL not available after {} retries - continuing without database", maxRetries);
                    }
                }
            }
        }

        public boolean isDatabaseReady() {
            return databaseReady;
        }
    }
}

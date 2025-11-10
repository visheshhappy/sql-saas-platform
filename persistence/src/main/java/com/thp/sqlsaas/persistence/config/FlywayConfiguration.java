package com.thp.sqlsaas.persistence.config;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flyway configuration for automatic database migrations.
 * This configuration ensures that Flyway migrations run automatically
 * when the application starts.
 */
@Configuration
public class FlywayConfiguration {

    /**
     * Defines the migration strategy for Flyway.
     * This will automatically migrate the database on application startup.
     */
    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            // Migrate the database
            flyway.migrate();
        };
    }
}

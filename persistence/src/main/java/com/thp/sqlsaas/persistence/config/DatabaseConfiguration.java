package com.thp.sqlsaas.persistence.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Database configuration for JPA and repositories.
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.thp.sqlsaas.persistence.repository")
@EntityScan(basePackages = "com.thp.sqlsaas.persistence.entity")
@EnableTransactionManagement
public class DatabaseConfiguration {
}

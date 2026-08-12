package com.novacommerce.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Runs against PostgreSQL when explicitly enabled on a Docker-capable machine. */
@Testcontainers(disabledWithoutDocker = true)
@EnabledIfSystemProperty(named = "runPostgresIntegrationTests", matches = "true")
class PostgresFlywayIntegrationTests {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17.5-alpine");

    @Test
    void appliesAuthSchemaMigrationsToPostgres() {
        Flyway flyway = Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .load();

        flyway.migrate();

        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("2");
    }
}

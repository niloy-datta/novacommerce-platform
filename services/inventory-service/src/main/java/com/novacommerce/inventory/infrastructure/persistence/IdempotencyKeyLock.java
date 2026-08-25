package com.novacommerce.inventory.infrastructure.persistence;
import java.sql.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class IdempotencyKeyLock {
    private final JdbcTemplate jdbc;
    private final DataSource dataSource;

    public IdempotencyKeyLock(JdbcTemplate j, DataSource d) {
        this.jdbc = j;
        this.dataSource = d;
    }

    public void acquire(String key) {
        try (Connection c = dataSource.getConnection()) {
            if ("PostgreSQL".equalsIgnoreCase(c.getMetaData().getDatabaseProductName())) {
                jdbc.execute("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", (PreparedStatement ps) -> {
                    ps.setString(1, key);
                    ps.execute();
                    return null;
                });
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot identify database", e);
        }
    }
}

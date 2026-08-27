package com.novacommerce.inventory.infrastructure.persistence;
import java.sql.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class IdempotencyKeyLock {
    private final JdbcTemplate jdbc;

    public IdempotencyKeyLock(JdbcTemplate j) {
        this.jdbc = j;
    }

    public void acquire(String key) {
        // JdbcTemplate participates in the surrounding @Transactional connection.
        // Opening a second connection here exhausts small pools under concurrent
        // reservations and makes the advisory lock independent of the business
        // transaction. PostgreSQL releases this transaction-scoped lock on commit.
        jdbc.execute("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", (PreparedStatement ps) -> {
            ps.setString(1, key);
            ps.execute();
            return null;
        });
    }
}

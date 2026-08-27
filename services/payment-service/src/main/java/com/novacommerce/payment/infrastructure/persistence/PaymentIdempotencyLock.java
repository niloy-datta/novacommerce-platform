package com.novacommerce.payment.infrastructure.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentIdempotencyLock {
    private final JdbcTemplate jdbc;
    public PaymentIdempotencyLock(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public void acquire(String key) {
        jdbc.queryForObject("select pg_advisory_xact_lock(hashtextextended(?, 0))", Long.class, key);
    }
}

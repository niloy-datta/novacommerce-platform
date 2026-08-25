package com.novacommerce.catalog.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.novacommerce.catalog.application.ProductQueryService;
import com.novacommerce.catalog.domain.brand.Brand;
import com.novacommerce.catalog.domain.product.Product;
import com.novacommerce.catalog.domain.product.ProductVariant;
import com.novacommerce.catalog.infrastructure.persistence.BrandRepository;
import com.novacommerce.catalog.infrastructure.persistence.ProductRepository;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Exercises the PostgreSQL-only schema and search path; skipped when Docker is unavailable. */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class CatalogPostgresIntegrationTests {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:17.5-alpine"));

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.data.redis.host", () -> "127.0.0.1");
        registry.add("spring.data.redis.port", () -> "6399");
    }

    @Autowired ProductRepository products;
    @Autowired BrandRepository brands;
    @Autowired ProductQueryService queries;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void clearDatabase() {
        products.deleteAll();
        brands.deleteAll();
    }

    @Test
    void migrationsAndPostgresFullTextSearchPreserveCatalogSemantics() {
        assertThat(jdbc.queryForObject("select version from flyway_schema_history where success order by installed_rank desc limit 1", String.class)).isEqualTo("2");
        String indexDefinition = jdbc.queryForObject(
            "select indexdef from pg_indexes where schemaname = 'public' and indexname = 'idx_products_search_vector'",
            String.class);
        assertThat(indexDefinition).containsIgnoringCase("using gin").contains("search_vector");

        Brand brand = brands.saveAndFlush(new Brand(UUID.randomUUID(), "Nova Labs", "nova-labs", "Developer hardware"));
        Product active = new Product(UUID.randomUUID(), "Aurora Mechanical Keyboard", "aurora-keyboard",
            "A quiet developer keyboard", "Designed for focused software engineering", brand);
        active.addVariant(new ProductVariant(UUID.randomUUID(), "AURORA-KB-01", "US layout",
            Map.of("switch", "tactile"), new BigDecimal("129.90"), "USD"));
        active.activate();
        products.saveAndFlush(active);
        products.saveAndFlush(new Product(UUID.randomUUID(), "Aurora Draft Accessory", "aurora-draft",
            "Not published", "Draft catalog content", brand));

        var result = queries.search("mechanical keyboard", null, null, null, null, "relevance", 0, 20);
        assertThat(result.items()).extracting(item -> item.slug()).containsExactly("aurora-keyboard");
        assertThat(result.totalElements()).isEqualTo(1);

        BigDecimal persistedPrice = jdbc.queryForObject(
            "select price_amount from product_variants where sku = 'AURORA-KB-01'", BigDecimal.class);
        assertThat(persistedPrice).isEqualByComparingTo("129.90");
        Integer matchingVectors = jdbc.queryForObject(
            "select count(*) from products where search_vector @@ websearch_to_tsquery('english', ?)",
            Integer.class, "mechanical keyboard");
        assertThat(matchingVectors).isEqualTo(1);
    }
}

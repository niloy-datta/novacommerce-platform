package com.novacommerce.catalog.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.novacommerce.catalog.domain.brand.Brand;
import com.novacommerce.catalog.domain.product.Product;
import com.novacommerce.catalog.domain.product.ProductVariant;
import com.novacommerce.catalog.infrastructure.persistence.BrandRepository;
import com.novacommerce.catalog.infrastructure.persistence.ProductRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ProductRepository products;
    @Autowired BrandRepository brands;

    @BeforeEach
    void clearDatabase() { products.deleteAll(); brands.deleteAll(); }

    @Test
    void publicSearchReturnsOnlyActiveProductsWithPagination() throws Exception {
        Brand brand = brands.saveAndFlush(new Brand(UUID.randomUUID(), "Acme", "acme", "Hardware"));
        Product active = new Product(UUID.randomUUID(), "Nova Keyboard", "nova-keyboard", "Quiet mechanical keyboard", "", brand);
        active.addVariant(new ProductVariant(UUID.randomUUID(), "NOVA-KB-1", "Standard", java.util.Map.of("layout", "US"), new BigDecimal("99.00"), "USD"));
        active.activate();
        products.saveAndFlush(active);
        Product draft = new Product(UUID.randomUUID(), "Draft Product", "draft-product", "Not public", "", brand);
        products.saveAndFlush(draft);

        mvc.perform(get("/api/v1/products?q=keyboard&page=0&size=1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].slug").value("nova-keyboard"))
            .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/v1/products/draft-product")).andExpect(status().isNotFound());
    }

    @Test
    void adminEndpointsRequireAdminAndCsrf() throws Exception {
        String body = "{\"name\":\"Acme\",\"slug\":\"acme\",\"description\":\"Hardware\"}";
        mvc.perform(post("/api/v1/admin/catalog/brands").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/admin/catalog/brands").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/catalog/brands").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.slug").value("acme"));
    }

    @Test
    void publicRoutesDoNotRequireAuthentication() throws Exception {
        mvc.perform(get("/api/v1/categories")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/brands")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health")).andExpect(status().is5xxServerError());
    }
}

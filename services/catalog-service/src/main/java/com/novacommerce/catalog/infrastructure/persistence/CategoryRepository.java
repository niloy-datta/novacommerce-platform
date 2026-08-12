package com.novacommerce.catalog.infrastructure.persistence;

import com.novacommerce.catalog.domain.category.Category;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Optional<Category> findBySlug(String slug);
    List<Category> findAllByActiveTrueOrderByNameAsc();
    @Query("select c.parent.id from Category c where c.id = :id") Optional<UUID> findParentIdById(UUID id);
    boolean existsBySlug(String slug);
}

package com.samosajunction.product.repository;

import com.samosajunction.product.dto.ProductSearchCriteria;
import com.samosajunction.product.entity.Product;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> from(ProductSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            boolean dataQuery = query.getResultType() != Long.class && query.getResultType() != long.class;
            if (dataQuery) {
                root.fetch("category", JoinType.INNER);
            }

            if (hasText(criteria.search())) {
                String pattern = "%" + criteria.search().trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }

            if (hasText(criteria.category())) {
                Path<String> categoryCode = dataQuery
                        ? root.get("category").get("code")
                        : root.join("category", JoinType.INNER).get("code");
                predicates.add(cb.equal(
                        cb.upper(categoryCode),
                        criteria.category().trim().toUpperCase(Locale.ROOT)
                ));
            }

            if (criteria.spiceLevel() != null) {
                predicates.add(cb.equal(root.get("spiceLevel"), criteria.spiceLevel()));
            }

            if (criteria.available() != null) {
                predicates.add(cb.equal(root.get("available"), criteria.available()));
            }

            if (hasText(criteria.dietaryTag())) {
                query.distinct(true);
                Join<Product, String> tags = root.join("dietaryTags", JoinType.INNER);
                predicates.add(cb.equal(cb.upper(tags), criteria.dietaryTag().trim().toUpperCase(Locale.ROOT)));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

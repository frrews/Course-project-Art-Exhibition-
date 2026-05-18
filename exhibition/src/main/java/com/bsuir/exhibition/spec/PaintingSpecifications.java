package com.bsuir.exhibition.spec;

import com.bsuir.exhibition.entity.Painting;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public final class PaintingSpecifications {

    private PaintingSpecifications() {
    }

    public static Specification<Painting> build(String q, String style, String author) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(q)) {
                String pattern = "%" + escapeLike(q.trim()) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern.toLowerCase(), '\\'),
                        cb.like(cb.lower(root.get("author")), pattern.toLowerCase(), '\\'),
                        cb.like(cb.lower(root.get("description")), pattern.toLowerCase(), '\\')
                ));
            }

            if (StringUtils.hasText(style)) {
                predicates.add(cb.equal(cb.lower(root.get("style")), style.trim().toLowerCase()));
            }

            if (StringUtils.hasText(author)) {
                String ap = "%" + escapeLike(author.trim()) + "%";
                predicates.add(cb.like(cb.lower(root.get("author")), ap.toLowerCase(), '\\'));
            }

            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String escapeLike(String raw) {
        return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}

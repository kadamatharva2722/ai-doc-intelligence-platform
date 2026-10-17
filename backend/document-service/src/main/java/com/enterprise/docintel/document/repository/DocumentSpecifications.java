package com.enterprise.docintel.document.repository;

import com.enterprise.docintel.document.entity.Document;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDateTime;

public class DocumentSpecifications {

    public static Specification<Document> hasNameLike(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isBlank()) return cb.conjunction();
            return cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
        };
    }

    public static Specification<Document> hasFileType(String fileType) {
        return (root, query, cb) -> {
            if (fileType == null || fileType.isBlank()) return cb.conjunction();
            return cb.equal(cb.upper(root.get("fileType")), fileType.toUpperCase());
        };
    }

    public static Specification<Document> hasOwnerUsernameLike(String ownerUsername) {
        return (root, query, cb) -> {
            if (ownerUsername == null || ownerUsername.isBlank()) return cb.conjunction();
            return cb.like(cb.lower(root.get("ownerUsername")), "%" + ownerUsername.toLowerCase() + "%");
        };
    }

    public static Specification<Document> createdAfterOrEqual(LocalDateTime startDate) {
        return (root, query, cb) -> {
            if (startDate == null) return cb.conjunction();
            return cb.greaterThanOrEqualTo(root.get("createdAt"), startDate);
        };
    }

    public static Specification<Document> createdBeforeOrEqual(LocalDateTime endDate) {
        return (root, query, cb) -> {
            if (endDate == null) return cb.conjunction();
            return cb.lessThanOrEqualTo(root.get("createdAt"), endDate);
        };
    }
}

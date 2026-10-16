package com.enterprise.docintel.repository;

import com.enterprise.docintel.entity.Document;
import com.enterprise.docintel.entity.User;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDateTime;

public class DocumentSpecifications {

    public static Specification<Document> hasNameLike(String name) {
        return (root, query, criteriaBuilder) -> {
            if (name == null || name.trim().isEmpty()) {
                return null;
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get("name")),
                "%" + name.trim().toLowerCase() + "%"
            );
        };
    }

    public static Specification<Document> hasFileType(String type) {
        return (root, query, criteriaBuilder) -> {
            if (type == null || type.trim().isEmpty()) {
                return null;
            }
            return criteriaBuilder.equal(
                criteriaBuilder.lower(root.get("fileType")),
                type.trim().toLowerCase()
            );
        };
    }

    public static Specification<Document> hasOwnerUsernameLike(String ownerUsername) {
        return (root, query, criteriaBuilder) -> {
            if (ownerUsername == null || ownerUsername.trim().isEmpty()) {
                return null;
            }
            Join<Document, User> ownerJoin = root.join("owner");
            return criteriaBuilder.like(
                criteriaBuilder.lower(ownerJoin.get("username")),
                "%" + ownerUsername.trim().toLowerCase() + "%"
            );
        };
    }

    public static Specification<Document> createdAfterOrEqual(LocalDateTime startDate) {
        return (root, query, criteriaBuilder) -> {
            if (startDate == null) {
                return null;
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startDate);
        };
    }

    public static Specification<Document> createdBeforeOrEqual(LocalDateTime endDate) {
        return (root, query, criteriaBuilder) -> {
            if (endDate == null) {
                return null;
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), endDate);
        };
    }
}

package com.backlog.backend.Specification;

import com.backlog.backend.entity.PostEntity;
import com.backlog.backend.entity.UserEntity;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

public class PostSpecification {
    public static Specification<PostEntity> search(String search) {
        return (root, query, criteriaBuilder) -> {
            if (search == null || search.isBlank()) {
                return criteriaBuilder.conjunction();
            }

            String value = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            Join<PostEntity, UserEntity> authorJoin = root.join("userPost");

            return criteriaBuilder.or(
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("title")),
                            value
                    ),
                    criteriaBuilder.like(
                            criteriaBuilder.lower(authorJoin.get("username")),
                            value
                    ),
                    criteriaBuilder.like(
                            criteriaBuilder.lower(authorJoin.get("displayName")),
                            value
                    )
            );
        };
    }
}

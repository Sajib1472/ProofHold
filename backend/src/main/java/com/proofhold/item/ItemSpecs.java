package com.proofhold.item;

import com.proofhold.domain.ItemCategory;
import com.proofhold.domain.ItemStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ItemSpecs {

    private ItemSpecs() {}

    public static Specification<Item> filter(Long locationId, ItemCategory category, ItemStatus status, String q) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (locationId != null) {
                predicates.add(cb.equal(root.get("location").get("id"), locationId));
            }
            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            } else {
                predicates.add(root.get("status").in(
                        ItemStatus.HELD,
                        ItemStatus.CLAIM_PENDING,
                        ItemStatus.VERIFIED,
                        ItemStatus.READY_FOR_PICKUP,
                        ItemStatus.EXPIRED));
            }
            if (q != null && !q.isBlank()) {
                String needle = q.trim().toLowerCase(Locale.ROOT);
                Predicate locationName = cb.like(cb.lower(root.get("location").get("name")), "%" + needle + "%");
                predicates.add(cb.or(locationName, categoryContains(root, cb, needle)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Predicate categoryContains(
            jakarta.persistence.criteria.Root<Item> root,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            String needle) {
        List<Predicate> matches = new ArrayList<>();
        for (ItemCategory candidate : ItemCategory.values()) {
            String name = candidate.name().toLowerCase(Locale.ROOT);
            String words = name.replace('_', ' ');
            if (name.contains(needle) || words.contains(needle)) {
                matches.add(cb.equal(root.get("category"), candidate));
            }
        }
        if (matches.isEmpty()) {
            return cb.disjunction();
        }
        return cb.or(matches.toArray(Predicate[]::new));
    }
}

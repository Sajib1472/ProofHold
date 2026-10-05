package com.proofhold.item;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.proofhold.domain.ItemStatus;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {

    @Override
    @EntityGraph(attributePaths = "location")
    Page<Item> findAll(Specification<Item> spec, Pageable pageable);

    List<Item> findByStatusInAndHoldUntilBefore(Collection<ItemStatus> statuses, Instant cutoff);
}

package com.proofhold.handoff;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface HandoffRepository extends JpaRepository<Handoff, Long> {

    Optional<Handoff> findByItemIdAndIdempotencyKey(Long itemId, UUID idempotencyKey);
}

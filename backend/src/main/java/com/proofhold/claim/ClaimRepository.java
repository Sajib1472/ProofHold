package com.proofhold.claim;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClaimRepository extends JpaRepository<Claim, Long> {

    Optional<Claim> findByItemIdAndIdempotencyKey(Long itemId, UUID idempotencyKey);

    List<Claim> findByItemId(Long itemId);
}

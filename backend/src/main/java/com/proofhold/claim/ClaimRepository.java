package com.proofhold.claim;

import com.proofhold.domain.ClaimStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClaimRepository extends JpaRepository<Claim, Long> {

    Optional<Claim> findByItemIdAndIdempotencyKey(Long itemId, UUID idempotencyKey);

    List<Claim> findByItemId(Long itemId);

    List<Claim> findByItemIdAndStatus(Long itemId, ClaimStatus status);

    long countByItemIdAndStatus(Long itemId, ClaimStatus status);

    Optional<Claim> findByItemIdAndClaimer_IdAndStatus(Long itemId, Long claimerId, ClaimStatus status);

    org.springframework.data.domain.Page<Claim> findByClaimer_Id(Long claimerId, org.springframework.data.domain.Pageable pageable);
}

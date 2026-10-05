package com.proofhold.claim;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClaimAnswerRepository extends JpaRepository<ClaimAnswer, ClaimAnswer.Key> {

    List<ClaimAnswer> findByClaimId(Long claimId);
}

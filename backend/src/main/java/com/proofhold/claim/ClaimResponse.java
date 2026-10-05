package com.proofhold.claim;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.proofhold.domain.ClaimStatus;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClaimResponse(
        Long id,
        Long itemId,
        Long claimerId,
        ClaimStatus status,
        Integer answerScore,
        String reason,
        Instant createdAt,
        Instant decidedAt) {

    public static ClaimResponse forClaimer(Claim claim) {
        return new ClaimResponse(
                claim.getId(),
                claim.getItem().getId(),
                claim.getClaimer().getId(),
                claim.getStatus(),
                null,
                claim.getReason(),
                claim.getCreatedAt(),
                claim.getDecidedAt());
    }

    public static ClaimResponse forStaff(Claim claim) {
        return new ClaimResponse(
                claim.getId(),
                claim.getItem().getId(),
                claim.getClaimer().getId(),
                claim.getStatus(),
                claim.getAnswerScore(),
                claim.getReason(),
                claim.getCreatedAt(),
                claim.getDecidedAt());
    }
}

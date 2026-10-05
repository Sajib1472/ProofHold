package com.proofhold.handoff;

import com.proofhold.domain.HandoffStatus;

import java.time.Instant;

public record HandoffResponse(
        Long id,
        Long itemId,
        Long claimId,
        Instant slotStart,
        Instant slotEnd,
        HandoffStatus status) {

    public static HandoffResponse from(Handoff handoff) {
        return new HandoffResponse(
                handoff.getId(),
                handoff.getItem().getId(),
                handoff.getClaim().getId(),
                handoff.getSlotStart(),
                handoff.getSlotEnd(),
                handoff.getStatus());
    }
}

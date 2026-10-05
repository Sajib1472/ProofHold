package com.proofhold.audit;

import com.proofhold.domain.AuditAction;

import java.time.Instant;
import java.util.Map;

public record AuditEventResponse(
        Long id, Long itemId, Long actorId, AuditAction action, Instant at, Map<String, Object> payload) {

    public static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getItem().getId(),
                event.getActor().getId(),
                event.getAction(),
                event.getAt(),
                event.getPayload());
    }
}

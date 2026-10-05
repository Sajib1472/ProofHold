package com.proofhold.audit;

import com.proofhold.domain.AuditAction;
import com.proofhold.item.Item;
import com.proofhold.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
public class AuditService {

    private final AuditEventRepository events;

    public AuditService(AuditEventRepository events) {
        this.events = events;
    }

    @Transactional
    public void record(Item item, User actor, AuditAction action, Map<String, Object> payload) {
        AuditEvent event = new AuditEvent();
        event.setItem(item);
        event.setActor(actor);
        event.setAction(action);
        event.setAt(Instant.now());
        event.setPayload(payload);
        events.save(event);
    }

    @Transactional(readOnly = true)
    public AuditPage list(Long itemId, int page, int size) {
        var pageable = org.springframework.data.domain.PageRequest.of(page, size);
        var result = events.findByItemIdOrderByAtAsc(itemId, pageable);
        return new AuditPage(
                result.getContent().stream().map(AuditEventResponse::from).toList(),
                new com.proofhold.item.PageInfo(
                        result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages()));
    }
}

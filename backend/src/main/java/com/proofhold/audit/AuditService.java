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
}

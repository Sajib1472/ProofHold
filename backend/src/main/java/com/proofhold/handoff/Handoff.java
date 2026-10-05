package com.proofhold.handoff;

import com.proofhold.claim.Claim;
import com.proofhold.domain.HandoffStatus;
import com.proofhold.item.Item;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "handoffs")
public class Handoff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "claim_id", nullable = false)
    private Claim claim;

    @Column(name = "slot_start", nullable = false)
    private Instant slotStart;

    @Column(name = "slot_end", nullable = false)
    private Instant slotEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HandoffStatus status;

    @Column(name = "idempotency_key")
    private UUID idempotencyKey;

    public Long getId() {
        return id;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Claim getClaim() {
        return claim;
    }

    public void setClaim(Claim claim) {
        this.claim = claim;
    }

    public Instant getSlotStart() {
        return slotStart;
    }

    public void setSlotStart(Instant slotStart) {
        this.slotStart = slotStart;
    }

    public Instant getSlotEnd() {
        return slotEnd;
    }

    public void setSlotEnd(Instant slotEnd) {
        this.slotEnd = slotEnd;
    }

    public HandoffStatus getStatus() {
        return status;
    }

    public void setStatus(HandoffStatus status) {
        this.status = status;
    }

    public UUID getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(UUID idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}

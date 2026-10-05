package com.proofhold.handoff;

import com.proofhold.audit.AuditService;
import com.proofhold.auth.AuthPrincipal;
import com.proofhold.auth.UnauthorizedException;
import com.proofhold.claim.Claim;
import com.proofhold.claim.ClaimRepository;
import com.proofhold.domain.AuditAction;
import com.proofhold.domain.ClaimStatus;
import com.proofhold.domain.HandoffStatus;
import com.proofhold.domain.ItemStateMachine;
import com.proofhold.domain.ItemStatus;
import com.proofhold.domain.Role;
import com.proofhold.item.Item;
import com.proofhold.item.ItemRepository;
import com.proofhold.location.Location;
import com.proofhold.user.User;
import com.proofhold.user.UserRepository;
import com.proofhold.web.ConflictException;
import com.proofhold.web.ETags;
import com.proofhold.web.NotFoundException;
import com.proofhold.web.PreconditionFailedException;
import com.proofhold.web.Problem;
import com.proofhold.web.ValidationFailedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class HandoffService {

    private final HandoffRepository handoffs;
    private final ItemRepository items;
    private final ClaimRepository claims;
    private final UserRepository users;
    private final AuditService audit;

    public HandoffService(
            HandoffRepository handoffs,
            ItemRepository items,
            ClaimRepository claims,
            UserRepository users,
            AuditService audit) {
        this.handoffs = handoffs;
        this.items = items;
        this.claims = claims;
        this.users = users;
        this.audit = audit;
    }

    @Transactional
    public HandoffSubmitResult create(
            AuthPrincipal principal, Long itemId, UUID idempotencyKey, String ifMatch, CreateHandoffRequest request) {
        User actor = users.findById(principal.id()).orElseThrow(UnauthorizedException::new);
        Item item = items.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item " + itemId + " does not exist."));
        Claim winner = claims
                .findByItemIdAndStatus(itemId, ClaimStatus.VERIFIED)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ConflictException(
                        "illegal-transition",
                        "Illegal transition",
                        "Item " + itemId + " has no verified claim."));
        if (principal.role() != Role.STAFF && !winner.getClaimer().getId().equals(principal.id())) {
            throw new org.springframework.security.access.AccessDeniedException("Not the winning claimer.");
        }
        requireVersion(item, ifMatch);
        if (item.getStatus() != ItemStatus.VERIFIED && item.getStatus() != ItemStatus.READY_FOR_PICKUP) {
            throw new ConflictException(
                    "illegal-transition",
                    "Illegal transition",
                    "Handoff requires a verified item.");
        }

        var existing = handoffs.findByItemIdAndIdempotencyKey(itemId, idempotencyKey);
        if (existing.isPresent()) {
            return new HandoffSubmitResult(HandoffResponse.from(existing.get()), false, version(item));
        }

        Instant now = Instant.now();
        if (request.slotStart() == null
                || request.slotEnd() == null
                || !request.slotStart().isAfter(now)
                || !request.slotEnd().isAfter(request.slotStart())) {
            throw new ValidationFailedException(
                    "Pickup slot is not valid.",
                    List.of(new Problem.FieldError("slotStart", "future", "Slot must be in the future with end after start.")));
        }
        Location desk = item.getLocation();
        if (!DeskHours.contains(desk, request.slotStart(), request.slotEnd())) {
            throw new ValidationFailedException(
                    "Pickup is outside desk hours.",
                    List.of(new Problem.FieldError(
                            "slotStart",
                            "outside_hours",
                            "Location " + desk.getName() + " is open " + desk.getOpenFrom() + "–" + desk.getOpenTo() + " "
                                    + desk.getTimezone() + ".")));
        }

        Handoff handoff = new Handoff();
        handoff.setItem(item);
        handoff.setClaim(winner);
        handoff.setSlotStart(request.slotStart());
        handoff.setSlotEnd(request.slotEnd());
        handoff.setStatus(HandoffStatus.BOOKED);
        handoff.setIdempotencyKey(idempotencyKey);
        handoff = handoffs.save(handoff);

        if (item.getStatus() == ItemStatus.VERIFIED) {
            item.setStatus(ItemStateMachine.require(ItemStatus.VERIFIED, ItemStatus.READY_FOR_PICKUP));
            items.save(item);
        }
        audit.record(item, actor, AuditAction.HANDOFF_BOOKED, Map.of("handoffId", handoff.getId()));
        return new HandoffSubmitResult(HandoffResponse.from(handoff), true, version(item));
    }

    @Transactional
    public HandoffSubmitResult complete(AuthPrincipal principal, Long handoffId, String ifMatch) {
        User staff = users.findById(principal.id()).orElseThrow(UnauthorizedException::new);
        Handoff handoff = handoffs.findById(handoffId)
                .orElseThrow(() -> new NotFoundException("Handoff " + handoffId + " does not exist."));
        Item item = handoff.getItem();
        requireVersion(item, ifMatch);
        if (handoff.getStatus() != HandoffStatus.BOOKED) {
            throw new ConflictException(
                    "illegal-transition", "Illegal transition", "Handoff is not booked.");
        }
        item.setStatus(ItemStateMachine.require(item.getStatus(), ItemStatus.RETURNED));
        handoff.setStatus(HandoffStatus.COMPLETED);
        items.save(item);
        handoffs.save(handoff);
        audit.record(item, staff, AuditAction.HANDOFF_COMPLETED, Map.of("handoffId", handoff.getId()));
        return new HandoffSubmitResult(HandoffResponse.from(handoff), false, version(item));
    }

    @Transactional(readOnly = true)
    public HandoffResponse get(AuthPrincipal principal, Long handoffId) {
        Handoff handoff = handoffs.findById(handoffId)
                .orElseThrow(() -> new NotFoundException("Handoff " + handoffId + " does not exist."));
        Claim winner = handoff.getClaim();
        if (principal.role() != Role.STAFF && !winner.getClaimer().getId().equals(principal.id())) {
            throw new org.springframework.security.access.AccessDeniedException("Not allowed to view this handoff.");
        }
        return HandoffResponse.from(handoff);
    }

    private static void requireVersion(Item item, String ifMatch) {
        int expected = ETags.parse(ifMatch);
        int current = item.getVersion() == null ? 0 : item.getVersion();
        if (expected != current) {
            throw new PreconditionFailedException("Item version does not match If-Match.");
        }
    }

    private static int version(Item item) {
        return item.getVersion() == null ? 0 : item.getVersion();
    }
}

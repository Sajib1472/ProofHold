package com.proofhold.item;

import com.proofhold.audit.AuditService;
import com.proofhold.auth.AuthPrincipal;
import com.proofhold.auth.UnauthorizedException;
import com.proofhold.claim.ClaimRepository;
import com.proofhold.domain.AnswerHasher;
import com.proofhold.domain.AuditAction;
import com.proofhold.domain.ClaimStatus;
import com.proofhold.domain.ItemCategory;
import com.proofhold.domain.ItemStateMachine;
import com.proofhold.domain.ItemStatus;
import com.proofhold.domain.Role;
import com.proofhold.location.Location;
import com.proofhold.location.LocationRepository;
import com.proofhold.user.User;
import com.proofhold.user.UserRepository;
import com.proofhold.web.ConflictException;
import com.proofhold.web.ETags;
import com.proofhold.web.NotFoundException;
import com.proofhold.web.PreconditionFailedException;
import com.proofhold.web.Problem;
import com.proofhold.web.ValidationFailedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ItemService {

    private final ItemRepository items;
    private final ItemSecretRepository secrets;
    private final ChallengeRepository challenges;
    private final LocationRepository locations;
    private final ClaimRepository claims;
    private final UserRepository users;
    private final AuditService audit;

    public ItemService(
            ItemRepository items,
            ItemSecretRepository secrets,
            ChallengeRepository challenges,
            LocationRepository locations,
            ClaimRepository claims,
            UserRepository users,
            AuditService audit) {
        this.items = items;
        this.secrets = secrets;
        this.challenges = challenges;
        this.locations = locations;
        this.claims = claims;
        this.users = users;
        this.audit = audit;
    }

    @Transactional
    public StaffItemResponse create(AuthPrincipal principal, CreateItemRequest request) {
        User actor = users.findById(principal.id()).orElseThrow(UnauthorizedException::new);
        Location location = locations
                .findById(request.locationId())
                .orElseThrow(() -> new NotFoundException("Location " + request.locationId() + " does not exist."));
        if (!request.holdUntil().isAfter(Instant.now())) {
            throw new ValidationFailedException(
                    "Item could not be logged.",
                    List.of(new Problem.FieldError("holdUntil", "future", "holdUntil must be in the future.")));
        }

        Item item = new Item();
        item.setLocation(location);
        item.setStatus(ItemStatus.LOGGED);
        item.setCategory(request.category());
        item.setFoundAt(request.foundAt());
        item.setHoldUntil(request.holdUntil());
        item.setWhereFound(request.whereFound());
        item.setStatus(ItemStateMachine.require(ItemStatus.LOGGED, ItemStatus.HELD));
        item = items.save(item);

        ItemSecret secret = new ItemSecret();
        secret.setItem(item);
        secret.setPhotoUrl(request.photoUrl());
        secret.setSerial(request.serial());
        secret.setUniqueMarks(request.uniqueMarks());
        secret.setFullDescription(request.fullDescription());
        secrets.save(secret);

        for (CreateItemRequest.CreateChallengeRequest row : request.challenges()) {
            Challenge challenge = new Challenge();
            challenge.setItem(item);
            challenge.setPrompt(row.prompt());
            challenge.setExpectedAnswerHash(AnswerHasher.hash(row.expectedAnswer()));
            challenges.save(challenge);
        }

        audit.record(item, actor, AuditAction.ITEM_LOGGED, Map.of("category", request.category().name()));
        return ItemViews.toStaff(item, secret, challenges.findByItemId(item.getId()), 0);
    }

    @Transactional(readOnly = true)
    public Object get(Long itemId, Optional<AuthPrincipal> caller) {
        Item item = requireItem(itemId);
        int pending = (int) claims.countByItemIdAndStatus(itemId, ClaimStatus.PENDING);
        if (caller.isPresent() && caller.get().role() == Role.STAFF) {
            return ItemViews.toStaff(item, secret(itemId), challenges.findByItemId(itemId), pending);
        }
        if (caller.isPresent() && isWinningClaimer(itemId, caller.get().id())) {
            return ItemViews.toVerifiedClaimer(item, secret(itemId), pending);
        }
        return ItemViews.toPublic(item, pending);
    }

    @Transactional(readOnly = true)
    public PublicItemPage list(
            Long locationId, ItemCategory category, ItemStatus status, String q, int page, int size) {
        if (size < 1 || size > 100 || page < 0) {
            throw new ValidationFailedException(
                    "Request is not valid.",
                    List.of(new Problem.FieldError("size", "range", "page must be >= 0 and size must be 1–100.")));
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("foundAt"), Sort.Order.desc("id")));
        Page<Item> result = items.findAll(ItemSpecs.filter(locationId, category, status, q), pageable);
        List<PublicItemResponse> content = result.getContent().stream()
                .map(item -> ItemViews.toPublic(
                        item, (int) claims.countByItemIdAndStatus(item.getId(), ClaimStatus.PENDING)))
                .toList();
        return new PublicItemPage(
                content,
                new PageInfo(result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages()));
    }

    Item requireItem(Long itemId) {
        return items.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item " + itemId + " does not exist."));
    }

    private ItemSecret secret(Long itemId) {
        return secrets.findById(itemId).orElse(null);
    }

    private boolean isWinningClaimer(Long itemId, Long claimerId) {
        return claims.findByItemIdAndClaimer_IdAndStatus(itemId, claimerId, ClaimStatus.VERIFIED).isPresent();
    }

    @Transactional
    public int expireDue(Instant now) {
        User actor = users.findByEmail("staff@proofhold.local").orElse(null);
        List<Item> due = items.findByStatusInAndHoldUntilBefore(
                List.of(ItemStatus.HELD, ItemStatus.CLAIM_PENDING), now);
        int count = 0;
        for (Item item : due) {
            item.setStatus(ItemStateMachine.require(item.getStatus(), ItemStatus.EXPIRED));
            items.save(item);
            if (actor != null) {
                audit.record(item, actor, AuditAction.ITEM_EXPIRED, Map.of());
            }
            count++;
        }
        return count;
    }

    @Transactional
    public StaffItemResponse donate(AuthPrincipal principal, Long itemId, String ifMatch) {
        User staff = users.findById(principal.id()).orElseThrow(UnauthorizedException::new);
        Item item = requireItem(itemId);
        int expected = ETags.parse(ifMatch);
        int current = item.getVersion() == null ? 0 : item.getVersion();
        if (expected != current) {
            throw new PreconditionFailedException("Item version does not match If-Match.");
        }
        if (item.getStatus() != ItemStatus.EXPIRED) {
            throw new ConflictException(
                    "illegal-transition",
                    "Illegal transition",
                    "Only EXPIRED items can be donated.");
        }
        item.setStatus(ItemStateMachine.require(ItemStatus.EXPIRED, ItemStatus.DONATED));
        items.save(item);
        audit.record(item, staff, AuditAction.ITEM_DONATED, Map.of());
        int pending = (int) claims.countByItemIdAndStatus(itemId, ClaimStatus.PENDING);
        return ItemViews.toStaff(item, secret(itemId), challenges.findByItemId(itemId), pending);
    }
}

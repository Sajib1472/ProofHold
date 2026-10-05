package com.proofhold.claim;

import com.proofhold.audit.AuditService;
import com.proofhold.auth.AuthPrincipal;
import com.proofhold.auth.UnauthorizedException;
import com.proofhold.domain.AnswerHasher;
import com.proofhold.domain.AuditAction;
import com.proofhold.domain.ClaimStatus;
import com.proofhold.domain.ItemStateMachine;
import com.proofhold.domain.ItemStatus;
import com.proofhold.item.Challenge;
import com.proofhold.item.ChallengePrompt;
import com.proofhold.item.ChallengeRepository;
import com.proofhold.item.Item;
import com.proofhold.item.ItemRepository;
import com.proofhold.item.ItemViews;
import com.proofhold.user.User;
import com.proofhold.user.UserRepository;
import com.proofhold.web.ConflictException;
import com.proofhold.web.ETags;
import com.proofhold.web.NotFoundException;
import com.proofhold.web.PreconditionFailedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ClaimService {

    private final ClaimRepository claims;
    private final ItemRepository items;
    private final ChallengeRepository challenges;
    private final UserRepository users;
    private final AuditService audit;
    private final ClaimAnswerRepository answers;

    public ClaimService(
            ClaimRepository claims,
            ItemRepository items,
            ChallengeRepository challenges,
            UserRepository users,
            AuditService audit,
            ClaimAnswerRepository answers) {
        this.claims = claims;
        this.items = items;
        this.challenges = challenges;
        this.users = users;
        this.audit = audit;
        this.answers = answers;
    }

    @Transactional(readOnly = true)
    public List<ChallengePrompt> prompts(Long itemId) {
        Item item = requireItem(itemId);
        if (!ItemViews.isClaimable(item.getStatus())) {
            throw new ConflictException(
                    "illegal-transition",
                    "Illegal transition",
                    "Item " + itemId + " is not accepting claims.");
        }
        return challenges.findByItemId(itemId).stream()
                .map(c -> new ChallengePrompt(c.getId(), c.getPrompt()))
                .toList();
    }

    @Transactional
    public ClaimSubmitResult submit(AuthPrincipal principal, Long itemId, UUID idempotencyKey, CreateClaimRequest request) {
        User claimer = users.findById(principal.id()).orElseThrow(UnauthorizedException::new);
        Item item = requireItem(itemId);
        rejectIfUnclaimable(item);

        String requestHash = fingerprint(request.answers());
        var existing = claims.findByItemIdAndIdempotencyKey(itemId, idempotencyKey);
        if (existing.isPresent()) {
            Claim claim = existing.get();
            if (!requestHash.equals(claim.getRequestHash())) {
                throw new ConflictException(
                        "idempotency-conflict",
                        "Idempotency conflict",
                        "Idempotency-Key was reused with a different body.");
            }
            return new ClaimSubmitResult(ClaimResponse.forClaimer(claim), false);
        }

        List<Challenge> itemChallenges = challenges.findByItemId(itemId);
        int score = score(itemChallenges, request.answers());

        Claim claim = new Claim();
        claim.setItem(item);
        claim.setClaimer(claimer);
        claim.setStatus(ClaimStatus.PENDING);
        claim.setIdempotencyKey(idempotencyKey);
        claim.setRequestHash(requestHash);
        claim.setAnswerScore(score);
        claim.setCreatedAt(Instant.now());
        claim = claims.save(claim);
        for (CreateClaimRequest.ClaimAnswer row : request.answers()) {
            ClaimAnswer stored = new ClaimAnswer();
            stored.setClaimId(claim.getId());
            stored.setChallengeId(row.challengeId());
            stored.setValue(row.value());
            answers.save(stored);
        }

        if (item.getStatus() == ItemStatus.HELD) {
            item.setStatus(ItemStateMachine.require(ItemStatus.HELD, ItemStatus.CLAIM_PENDING));
            items.save(item);
        }

        audit.record(item, claimer, AuditAction.CLAIM_SUBMITTED, Map.of("claimId", claim.getId()));
        return new ClaimSubmitResult(ClaimResponse.forClaimer(claim), true);
    }

    @Transactional
    public ClaimDecisionResponse decide(
            AuthPrincipal principal, Long claimId, String ifMatch, ClaimDecisionRequest request) {
        User staff = users.findById(principal.id()).orElseThrow(UnauthorizedException::new);
        Claim claim = claims.findById(claimId)
                .orElseThrow(() -> new NotFoundException("Claim " + claimId + " does not exist."));
        Item item = claim.getItem();
        if (item.getStatus() == ItemStatus.RETURNED || item.getStatus() == ItemStatus.DONATED) {
            throw new ConflictException(
                    "illegal-transition",
                    "Illegal transition",
                    item.getStatus() + " cannot accept a claim decision.");
        }
        int expectedVersion = ETags.parse(ifMatch);
        int currentVersion = item.getVersion() == null ? 0 : item.getVersion();
        if (expectedVersion != currentVersion) {
            throw new PreconditionFailedException("Item version does not match If-Match.");
        }
        if (claim.getStatus() != ClaimStatus.PENDING) {
            throw new ConflictException(
                    "illegal-transition",
                    "Illegal transition",
                    "Claim " + claimId + " is not pending.");
        }

        Instant now = Instant.now();
        if (request.decision() == ClaimDecision.APPROVE) {
            item.setStatus(ItemStateMachine.require(item.getStatus(), ItemStatus.VERIFIED));
            claim.setStatus(ClaimStatus.VERIFIED);
            claim.setReason(request.reason());
            claim.setDecidedAt(now);
            for (Claim other : claims.findByItemIdAndStatus(item.getId(), ClaimStatus.PENDING)) {
                if (!claim.getId().equals(other.getId())) {
                    other.setStatus(ClaimStatus.REJECTED);
                    other.setReason("another claim was verified");
                    other.setDecidedAt(now);
                }
            }
            audit.record(item, staff, AuditAction.CLAIM_APPROVED, Map.of("claimId", claim.getId()));
        } else {
            claim.setStatus(ClaimStatus.REJECTED);
            claim.setReason(request.reason());
            claim.setDecidedAt(now);
            boolean othersPending = claims.findByItemIdAndStatus(item.getId(), ClaimStatus.PENDING).stream()
                    .anyMatch(other -> !claim.getId().equals(other.getId()));
            if (!othersPending) {
                item.setStatus(ItemStateMachine.require(item.getStatus(), ItemStatus.HELD));
            }
            audit.record(item, staff, AuditAction.CLAIM_REJECTED, Map.of("claimId", claim.getId()));
        }
        items.save(item);
        claims.save(claim);
        int version = item.getVersion() == null ? 0 : item.getVersion();
        return new ClaimDecisionResponse(ClaimResponse.forStaff(claim), version);
    }

    private void rejectIfUnclaimable(Item item) {
        ItemStatus status = item.getStatus();
        if (ItemViews.isClaimable(status)) {
            return;
        }
        if (status == ItemStatus.VERIFIED
                || status == ItemStatus.READY_FOR_PICKUP
                || status == ItemStatus.RETURNED) {
            throw new ConflictException(
                    "item-already-verified",
                    "Item already verified",
                    "Item " + item.getId() + " already has a verified claim.");
        }
        throw new ConflictException(
                "illegal-transition",
                "Illegal transition",
                "Item " + item.getId() + " cannot accept a claim in status " + status + ".");
    }

    private Item requireItem(Long itemId) {
        return items.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item " + itemId + " does not exist."));
    }

    static String fingerprint(List<CreateClaimRequest.ClaimAnswer> answers) {
        String canonical = answers.stream()
                .sorted(Comparator.comparing(CreateClaimRequest.ClaimAnswer::challengeId))
                .map(a -> a.challengeId() + "=" + AnswerHasher.normalize(a.value()))
                .collect(Collectors.joining("\n"));
        return AnswerHasher.sha256(canonical);
    }

    static int score(List<Challenge> challenges, List<CreateClaimRequest.ClaimAnswer> answers) {
        Map<Long, String> byId = answers.stream()
                .collect(Collectors.toMap(
                        CreateClaimRequest.ClaimAnswer::challengeId,
                        a -> AnswerHasher.hash(a.value()),
                        (a, b) -> b));
        int matches = 0;
        for (Challenge challenge : challenges) {
            if (challenge.getExpectedAnswerHash().equals(byId.get(challenge.getId()))) {
                matches++;
            }
        }
        return matches;
    }

    @Transactional(readOnly = true)
    public ClaimListPage listMine(AuthPrincipal principal, int page, int size) {
        var pageable = org.springframework.data.domain.PageRequest.of(
                page, size, org.springframework.data.domain.Sort.by("createdAt").descending());
        var result = claims.findByClaimer_Id(principal.id(), pageable);
        return new ClaimListPage(
                result.getContent().stream().map(ClaimResponse::forClaimer).toList(),
                new com.proofhold.item.PageInfo(
                        result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages()));
    }

    @Transactional(readOnly = true)
    public List<StaffClaimView> listForItem(Long itemId) {
        requireItem(itemId);
        List<Challenge> itemChallenges = challenges.findByItemId(itemId);
        Map<Long, String> prompts = itemChallenges.stream().collect(Collectors.toMap(Challenge::getId, Challenge::getPrompt));
        return claims.findByItemId(itemId).stream()
                .map(claim -> new StaffClaimView(
                        ClaimResponse.forStaff(claim),
                        answers.findByClaimId(claim.getId()).stream()
                                .map(a -> new StaffClaimView.Answer(a.getChallengeId(), prompts.get(a.getChallengeId()), a.getValue()))
                                .toList()))
                .toList();
    }
}

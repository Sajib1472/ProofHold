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
import com.proofhold.web.NotFoundException;
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

    public ClaimService(
            ClaimRepository claims,
            ItemRepository items,
            ChallengeRepository challenges,
            UserRepository users,
            AuditService audit) {
        this.claims = claims;
        this.items = items;
        this.challenges = challenges;
        this.users = users;
        this.audit = audit;
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

        if (item.getStatus() == ItemStatus.HELD) {
            item.setStatus(ItemStateMachine.require(ItemStatus.HELD, ItemStatus.CLAIM_PENDING));
            items.save(item);
        }

        audit.record(item, claimer, AuditAction.CLAIM_SUBMITTED, Map.of("claimId", claim.getId()));
        return new ClaimSubmitResult(ClaimResponse.forClaimer(claim), true);
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
}

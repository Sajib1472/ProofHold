package com.proofhold.claim;

import com.proofhold.audit.AuditService;
import com.proofhold.auth.AuthPrincipal;
import com.proofhold.domain.ClaimStatus;
import com.proofhold.domain.ItemStatus;
import com.proofhold.domain.Role;
import com.proofhold.item.ChallengeRepository;
import com.proofhold.item.Item;
import com.proofhold.item.ItemRepository;
import com.proofhold.item.ItemViewsRedactionTest;
import com.proofhold.user.User;
import com.proofhold.user.UserRepository;
import com.proofhold.web.ConflictException;
import com.proofhold.web.PreconditionFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClaimServiceDecisionTest {

    @Mock
    ClaimRepository claims;
    @Mock
    ItemRepository items;
    @Mock
    ChallengeRepository challenges;
    @Mock
    UserRepository users;
    @Mock
    AuditService audit;
    @Mock
    ClaimAnswerRepository answers;

    @InjectMocks
    ClaimService service;

    private Item item;
    private User staff;
    private User aliceUser;
    private User bobUser;
    private Claim alice;
    private Claim bob;

    @BeforeEach
    void setup() {
        item = ItemViewsRedactionTest.wallet();
        item.setStatus(ItemStatus.CLAIM_PENDING);
        item.setVersion(3);
        staff = user(1L, "staff@proofhold.local", Role.STAFF);
        aliceUser = user(2L, "alice@proofhold.local", Role.CLAIMER);
        bobUser = user(3L, "bob@proofhold.local", Role.CLAIMER);
        alice = pending(10L, aliceUser);
        bob = pending(11L, bobUser);
        when(users.findById(1L)).thenReturn(Optional.of(staff));
        when(claims.findById(10L)).thenReturn(Optional.of(alice));
        when(claims.findByItemIdAndStatus(18L, ClaimStatus.PENDING)).thenReturn(List.of(alice, bob));
        when(items.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(claims.save(any(Claim.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void approvingAliceRejectsBob() {
        ClaimDecisionResponse response = service.decide(
                new AuthPrincipal(1L, "staff@proofhold.local", Role.STAFF),
                10L,
                "\"3\"",
                new ClaimDecisionRequest(ClaimDecision.APPROVE, "matches"));

        assertEquals(ClaimStatus.VERIFIED, alice.getStatus());
        assertEquals(ClaimStatus.REJECTED, bob.getStatus());
        assertEquals("another claim was verified", bob.getReason());
        assertEquals(ItemStatus.VERIFIED, item.getStatus());
        assertEquals(ClaimStatus.VERIFIED, response.claim().status());
    }

    @Test
    void staleIfMatchFailsTheSecondApprove() {
        assertThrows(
                PreconditionFailedException.class,
                () -> service.decide(
                        new AuthPrincipal(1L, "staff@proofhold.local", Role.STAFF),
                        10L,
                        "\"2\"",
                        new ClaimDecisionRequest(ClaimDecision.APPROVE, "stale")));
    }

    @Test
    void decisionOnReturnedItemFails() {
        item.setStatus(ItemStatus.RETURNED);

        ConflictException ex = assertThrows(
                ConflictException.class,
                () -> service.decide(
                        new AuthPrincipal(1L, "staff@proofhold.local", Role.STAFF),
                        10L,
                        "\"3\"",
                        new ClaimDecisionRequest(ClaimDecision.APPROVE, "too late")));

        assertEquals("illegal-transition", ex.getSlug());
    }

    private Claim pending(Long id, User claimer) {
        Claim claim = new Claim();
        claim.setId(id);
        claim.setItem(item);
        claim.setClaimer(claimer);
        claim.setStatus(ClaimStatus.PENDING);
        claim.setIdempotencyKey(UUID.randomUUID());
        claim.setRequestHash("hash");
        claim.setCreatedAt(Instant.parse("2026-10-05T16:00:00Z"));
        return claim;
    }

    private static User user(Long id, String email, Role role) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setRole(role);
        return user;
    }
}

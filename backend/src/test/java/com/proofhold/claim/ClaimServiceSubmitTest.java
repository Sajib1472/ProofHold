package com.proofhold.claim;

import com.proofhold.audit.AuditService;
import com.proofhold.auth.AuthPrincipal;
import com.proofhold.domain.AnswerHasher;
import com.proofhold.domain.ClaimStatus;
import com.proofhold.domain.ItemStatus;
import com.proofhold.domain.Role;
import com.proofhold.item.Challenge;
import com.proofhold.item.ChallengeRepository;
import com.proofhold.item.Item;
import com.proofhold.item.ItemRepository;
import com.proofhold.item.ItemViewsRedactionTest;
import com.proofhold.user.User;
import com.proofhold.user.UserRepository;
import com.proofhold.web.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClaimServiceSubmitTest {

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

    @InjectMocks
    ClaimService service;

    private final UUID key = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private Item item;
    private User alice;
    private Challenge initials;
    private Challenge cards;
    private CreateClaimRequest body;

    @BeforeEach
    void setup() {
        item = ItemViewsRedactionTest.wallet();
        alice = new User();
        alice.setId(2L);
        alice.setEmail("alice@proofhold.local");
        alice.setRole(Role.CLAIMER);
        initials = new Challenge();
        initials.setId(3L);
        initials.setPrompt("What initials are inside?");
        initials.setExpectedAnswerHash(AnswerHasher.hash("JS"));
        cards = new Challenge();
        cards.setId(4L);
        cards.setPrompt("About how many cards?");
        cards.setExpectedAnswerHash(AnswerHasher.hash("8"));
        body = new CreateClaimRequest(List.of(
                new CreateClaimRequest.ClaimAnswer(3L, "JS"),
                new CreateClaimRequest.ClaimAnswer(4L, "8")));
        when(users.findById(2L)).thenReturn(Optional.of(alice));
        when(items.findById(18L)).thenReturn(Optional.of(item));
        when(challenges.findByItemId(18L)).thenReturn(List.of(initials, cards));
        when(claims.findByItemIdAndIdempotencyKey(18L, key)).thenReturn(Optional.empty());
        when(claims.save(any(Claim.class))).thenAnswer(invocation -> {
            Claim claim = invocation.getArgument(0);
            claim.setId(9L);
            return claim;
        });
    }

    @Test
    void aliceCanClaimItem() {
        ClaimSubmitResult result = service.submit(
                new AuthPrincipal(2L, "alice@proofhold.local", Role.CLAIMER), 18L, key, body);

        assertTrue(result.created());
        assertEquals(ClaimStatus.PENDING, result.claim().status());
        assertEquals(ItemStatus.CLAIM_PENDING, item.getStatus());
        verify(claims).save(any(Claim.class));
    }

    @Test
    void sameIdempotencyKeyDoesNotCreateASecondRow() {
        Claim existing = new Claim();
        existing.setId(9L);
        existing.setItem(item);
        existing.setClaimer(alice);
        existing.setStatus(ClaimStatus.PENDING);
        existing.setIdempotencyKey(key);
        existing.setRequestHash(ClaimService.fingerprint(body.answers()));
        existing.setCreatedAt(java.time.Instant.parse("2026-10-05T15:00:00Z"));
        when(claims.findByItemIdAndIdempotencyKey(18L, key)).thenReturn(Optional.of(existing));

        ClaimSubmitResult result = service.submit(
                new AuthPrincipal(2L, "alice@proofhold.local", Role.CLAIMER), 18L, key, body);

        assertFalse(result.created());
        assertEquals(9L, result.claim().id());
        verify(claims, never()).save(any(Claim.class));
    }

    @Test
    void claimingReturnedItemIs409() {
        item.setStatus(ItemStatus.RETURNED);

        ConflictException ex = assertThrows(
                ConflictException.class,
                () -> service.submit(
                        new AuthPrincipal(2L, "alice@proofhold.local", Role.CLAIMER), 18L, key, body));

        assertEquals("item-already-verified", ex.getSlug());
        verify(claims, never()).save(any(Claim.class));
    }
}

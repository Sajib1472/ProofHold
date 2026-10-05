package com.proofhold.item;

import com.proofhold.audit.AuditService;
import com.proofhold.claim.ClaimRepository;
import com.proofhold.domain.ItemStatus;
import com.proofhold.location.LocationRepository;
import com.proofhold.user.User;
import com.proofhold.user.UserRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ItemServiceExpiryTest {

    @Mock
    ItemRepository items;
    @Mock
    ItemSecretRepository secrets;
    @Mock
    ChallengeRepository challenges;
    @Mock
    LocationRepository locations;
    @Mock
    ClaimRepository claims;
    @Mock
    UserRepository users;
    @Mock
    AuditService audit;

    @InjectMocks
    ItemService service;

    @Test
    void pastHoldUntilMovesHeldItemToExpired() {
        Item item = ItemViewsRedactionTest.wallet();
        item.setHoldUntil(Instant.parse("2020-01-01T00:00:00Z"));
        item.setStatus(ItemStatus.HELD);
        when(items.findByStatusInAndHoldUntilBefore(any(), any())).thenReturn(List.of(item));
        when(users.findByEmail("staff@proofhold.local")).thenReturn(Optional.of(new User()));
        when(items.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int moved = service.expireDue(Instant.parse("2026-10-05T00:00:00Z"));

        assertEquals(1, moved);
        assertEquals(ItemStatus.EXPIRED, item.getStatus());
    }
}

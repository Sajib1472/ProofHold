package com.proofhold.item;

import com.proofhold.audit.AuditService;
import com.proofhold.auth.AuthPrincipal;
import com.proofhold.claim.ClaimRepository;
import com.proofhold.domain.AuditAction;
import com.proofhold.domain.ItemCategory;
import com.proofhold.domain.ItemStatus;
import com.proofhold.domain.Role;
import com.proofhold.location.Location;
import com.proofhold.location.LocationRepository;
import com.proofhold.user.User;
import com.proofhold.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ItemServiceLogTest {

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

    @BeforeEach
    void stubs() {
        Location desk = new Location();
        desk.setId(1L);
        desk.setName("Library Front Desk");
        desk.setTimezone("America/New_York");
        desk.setOpenFrom(LocalTime.of(9, 0));
        desk.setOpenTo(LocalTime.of(17, 0));
        User staff = new User();
        staff.setId(1L);
        staff.setEmail("staff@proofhold.local");
        staff.setRole(Role.STAFF);
        when(users.findById(1L)).thenReturn(Optional.of(staff));
        when(locations.findById(1L)).thenReturn(Optional.of(desk));
        when(items.save(any(Item.class))).thenAnswer(invocation -> {
            Item item = invocation.getArgument(0);
            item.setId(18L);
            item.setVersion(0);
            return item;
        });
        AtomicLong challengeIds = new AtomicLong(1);
        when(challenges.save(any(Challenge.class))).thenAnswer(invocation -> {
            Challenge challenge = invocation.getArgument(0);
            challenge.setId(challengeIds.getAndIncrement());
            return challenge;
        });
        when(challenges.findByItemId(18L)).thenAnswer(invocation -> List.of());
    }

    @Test
    void staffCanLogBlackLeatherWalletWithTwoQuestions() {
        CreateItemRequest request = new CreateItemRequest(
                1L,
                ItemCategory.WALLET,
                Instant.now().minus(2, ChronoUnit.HOURS),
                Instant.now().plus(30, ChronoUnit.DAYS),
                "2nd floor",
                "https://desk.local/wallet.jpg",
                "WL-9",
                "initials JS",
                "black leather wallet",
                List.of(
                        new CreateItemRequest.CreateChallengeRequest("What initials are inside?", "JS"),
                        new CreateItemRequest.CreateChallengeRequest("About how many cards?", "8")));

        StaffItemResponse body = service.create(new AuthPrincipal(1L, "staff@proofhold.local", Role.STAFF), request);

        assertEquals(ItemStatus.HELD, body.status());
        assertEquals(ItemCategory.WALLET, body.category());
        assertEquals("black leather wallet", body.fullDescription());
        ArgumentCaptor<Item> item = ArgumentCaptor.forClass(Item.class);
        verify(items).save(item.capture());
        assertEquals(ItemStatus.HELD, item.getValue().getStatus());
        verify(audit).record(any(), any(), eq(AuditAction.ITEM_LOGGED), any());
        verify(challenges, org.mockito.Mockito.times(2)).save(any(Challenge.class));
    }

    @Test
    void publicGetHidesSecretsThatStaffSee() {
        Item item = ItemViewsRedactionTest.wallet();
        when(items.findById(18L)).thenReturn(Optional.of(item));
        when(secrets.findById(18L)).thenReturn(Optional.of(ItemViewsRedactionTest.secret()));
        when(challenges.findByItemId(18L)).thenReturn(List.of(ItemViewsRedactionTest.challenge()));
        when(claims.countByItemIdAndStatus(eq(18L), any())).thenReturn(2L);

        PublicItemResponse publicView =
                (PublicItemResponse) service.get(18L, Optional.empty());
        StaffItemResponse staffView = (StaffItemResponse)
                service.get(18L, Optional.of(new AuthPrincipal(1L, "staff@proofhold.local", Role.STAFF)));

        assertEquals(ItemStatus.HELD, publicView.status());
        assertEquals("https://desk.local/wallet.jpg", staffView.photoUrl());
        assertEquals("WL-9", staffView.serial());
        assertEquals("black leather wallet", staffView.fullDescription());
    }
}

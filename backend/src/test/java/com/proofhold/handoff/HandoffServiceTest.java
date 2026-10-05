package com.proofhold.handoff;

import com.proofhold.audit.AuditService;
import com.proofhold.auth.AuthPrincipal;
import com.proofhold.claim.Claim;
import com.proofhold.claim.ClaimRepository;
import com.proofhold.domain.ClaimStatus;
import com.proofhold.domain.HandoffStatus;
import com.proofhold.domain.ItemStatus;
import com.proofhold.domain.Role;
import com.proofhold.item.Item;
import com.proofhold.item.ItemRepository;
import com.proofhold.item.ItemViewsRedactionTest;
import com.proofhold.user.User;
import com.proofhold.user.UserRepository;
import com.proofhold.web.ValidationFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HandoffServiceTest {

    @Mock
    HandoffRepository handoffs;
    @Mock
    ItemRepository items;
    @Mock
    ClaimRepository claims;
    @Mock
    UserRepository users;
    @Mock
    AuditService audit;

    @InjectMocks
    HandoffService service;

    private Item item;
    private User alice;
    private User staff;
    private Claim winner;
    private final UUID key = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @BeforeEach
    void setup() {
        item = ItemViewsRedactionTest.wallet();
        item.setStatus(ItemStatus.VERIFIED);
        item.setVersion(1);
        alice = new User();
        alice.setId(2L);
        alice.setEmail("alice@proofhold.local");
        alice.setRole(Role.CLAIMER);
        staff = new User();
        staff.setId(1L);
        staff.setEmail("staff@proofhold.local");
        staff.setRole(Role.STAFF);
        winner = new Claim();
        winner.setId(10L);
        winner.setItem(item);
        winner.setClaimer(alice);
        winner.setStatus(ClaimStatus.VERIFIED);
        when(users.findById(2L)).thenReturn(Optional.of(alice));
        when(users.findById(1L)).thenReturn(Optional.of(staff));
        when(items.findById(18L)).thenReturn(Optional.of(item));
        when(claims.findByItemIdAndStatus(18L, ClaimStatus.VERIFIED)).thenReturn(List.of(winner));
        when(handoffs.findByItemIdAndIdempotencyKey(18L, key)).thenReturn(Optional.empty());
        when(handoffs.save(any(Handoff.class))).thenAnswer(invocation -> {
            Handoff handoff = invocation.getArgument(0);
            handoff.setId(44L);
            return handoff;
        });
        when(items.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void pickupOutsideOpenHoursIs422() {
        ZonedDateTime evening = nextWeekday(20, 0);
        ValidationFailedException ex = assertThrows(
                ValidationFailedException.class,
                () -> service.create(
                        new AuthPrincipal(2L, "alice@proofhold.local", Role.CLAIMER),
                        18L,
                        key,
                        "\"1\"",
                        new CreateHandoffRequest(evening.toInstant(), evening.plusHours(1).toInstant())));
        assertEquals("outside_hours", ex.getErrors().getFirst().code());
    }

    @Test
    void completingHandoffSetsReturned() {
        Handoff booked = new Handoff();
        booked.setId(44L);
        booked.setItem(item);
        booked.setClaim(winner);
        booked.setStatus(HandoffStatus.BOOKED);
        booked.setSlotStart(nextWeekday(10, 0).toInstant());
        booked.setSlotEnd(nextWeekday(11, 0).toInstant());
        item.setStatus(ItemStatus.READY_FOR_PICKUP);
        when(handoffs.findById(44L)).thenReturn(Optional.of(booked));

        HandoffSubmitResult result = service.complete(
                new AuthPrincipal(1L, "staff@proofhold.local", Role.STAFF), 44L, "\"1\"");

        assertEquals(ItemStatus.RETURNED, item.getStatus());
        assertEquals(HandoffStatus.COMPLETED, result.handoff().status());
    }

    private static ZonedDateTime nextWeekday(int hour, int minute) {
        ZoneId zone = ZoneId.of("America/New_York");
        ZonedDateTime candidate = LocalDate.now(zone).plusDays(1).atTime(hour, minute).atZone(zone);
        while (candidate.getDayOfWeek().getValue() >= 6) {
            candidate = candidate.plusDays(1);
        }
        return candidate;
    }
}

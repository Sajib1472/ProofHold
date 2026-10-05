package com.proofhold.handoff;

import com.proofhold.item.ItemViewsRedactionTest;
import com.proofhold.location.Location;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeskHoursTest {

    @Test
    void morningSlotInsideLibraryHours() {
        Location desk = ItemViewsRedactionTest.wallet().getLocation();
        ZonedDateTime start = LocalDate.of(2026, 10, 6).atTime(10, 0).atZone(ZoneId.of("America/New_York"));
        assertTrue(DeskHours.contains(desk, start.toInstant(), start.plusHours(1).toInstant()));
    }

    @Test
    void eveningSlotOutsideLibraryHours() {
        Location desk = ItemViewsRedactionTest.wallet().getLocation();
        ZonedDateTime start = LocalDate.of(2026, 10, 6).atTime(20, 0).atZone(ZoneId.of("America/New_York"));
        assertFalse(DeskHours.contains(desk, start.toInstant(), start.plusHours(1).toInstant()));
    }
}

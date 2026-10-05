package com.proofhold.handoff;

import com.proofhold.location.Location;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class DeskHours {

    private DeskHours() {}

    public static boolean contains(Location location, Instant slotStart, Instant slotEnd) {
        ZoneId zone = ZoneId.of(location.getTimezone());
        ZonedDateTime start = slotStart.atZone(zone);
        ZonedDateTime end = slotEnd.atZone(zone);
        if (!start.toLocalDate().equals(end.toLocalDate())) {
            return false;
        }
        LocalTime from = location.getOpenFrom();
        LocalTime to = location.getOpenTo();
        LocalTime startTime = start.toLocalTime();
        LocalTime endTime = end.toLocalTime();
        return !startTime.isBefore(from) && !endTime.isAfter(to) && endTime.isAfter(startTime);
    }
}

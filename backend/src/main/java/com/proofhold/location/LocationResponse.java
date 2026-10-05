package com.proofhold.location;

import java.time.LocalTime;

public record LocationResponse(Long id, String name, String timezone, LocalTime openFrom, LocalTime openTo) {

    public static LocationResponse from(Location location) {
        return new LocationResponse(
                location.getId(),
                location.getName(),
                location.getTimezone(),
                location.getOpenFrom(),
                location.getOpenTo());
    }
}

package com.proofhold.item;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.proofhold.domain.ItemCategory;
import com.proofhold.domain.ItemStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record StaffItemResponse(
        Long id,
        Long locationId,
        String locationName,
        ItemCategory category,
        LocalDate foundOn,
        ItemStatus status,
        int pendingClaimCount,
        int version,
        Instant holdUntil,
        String whereFound,
        String photoUrl,
        String serial,
        String uniqueMarks,
        String fullDescription,
        List<StaffChallengeResponse> challenges) {}

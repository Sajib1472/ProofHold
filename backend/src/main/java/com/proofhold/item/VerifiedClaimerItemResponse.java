package com.proofhold.item;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.proofhold.domain.ItemCategory;
import com.proofhold.domain.ItemStatus;

import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record VerifiedClaimerItemResponse(
        Long id,
        Long locationId,
        String locationName,
        ItemCategory category,
        LocalDate foundOn,
        ItemStatus status,
        int pendingClaimCount,
        int version,
        String photoUrl,
        String serial,
        String uniqueMarks,
        String fullDescription) {}

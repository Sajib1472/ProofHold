package com.proofhold.item;

import com.proofhold.domain.ItemCategory;
import com.proofhold.domain.ItemStatus;

import java.time.LocalDate;

public record PublicItemResponse(
        Long id,
        Long locationId,
        String locationName,
        ItemCategory category,
        LocalDate foundOn,
        ItemStatus status,
        int pendingClaimCount,
        int version) {}

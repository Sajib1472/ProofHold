package com.proofhold.item;

import com.proofhold.domain.ItemCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public record CreateItemRequest(
        @NotNull Long locationId,
        @NotNull ItemCategory category,
        @NotNull Instant foundAt,
        @NotNull @Future Instant holdUntil,
        @NotBlank @Size(max = 200) String whereFound,
        String photoUrl,
        @Size(max = 80) String serial,
        @Size(max = 200) String uniqueMarks,
        @NotBlank @Size(max = 2000) String fullDescription,
        @NotNull @Size(min = 2, max = 5) @Valid List<CreateChallengeRequest> challenges) {

    public record CreateChallengeRequest(
            @NotBlank @Size(min = 4, max = 200) String prompt,
            @NotBlank @Size(min = 1, max = 80) String expectedAnswer) {}
}

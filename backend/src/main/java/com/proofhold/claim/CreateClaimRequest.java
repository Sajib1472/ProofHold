package com.proofhold.claim;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateClaimRequest(
        @NotNull @Size(min = 2) @Valid List<ClaimAnswer> answers) {

    public record ClaimAnswer(
            @NotNull Long challengeId,
            @NotBlank @Size(max = 80) String value) {}
}

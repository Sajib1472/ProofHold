package com.proofhold.claim;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClaimDecisionRequest(
        @NotNull ClaimDecision decision,
        @Size(max = 500) String reason) {}

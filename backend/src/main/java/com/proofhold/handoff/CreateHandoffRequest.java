package com.proofhold.handoff;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreateHandoffRequest(@NotNull Instant slotStart, @NotNull Instant slotEnd) {}

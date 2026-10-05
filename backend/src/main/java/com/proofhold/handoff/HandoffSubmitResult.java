package com.proofhold.handoff;

public record HandoffSubmitResult(HandoffResponse handoff, boolean created, int itemVersion) {}

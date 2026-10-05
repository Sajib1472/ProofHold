package com.proofhold.claim;

import java.util.List;

public record StaffClaimView(ClaimResponse claim, List<Answer> answers) {

    public record Answer(Long challengeId, String prompt, String value) {}
}

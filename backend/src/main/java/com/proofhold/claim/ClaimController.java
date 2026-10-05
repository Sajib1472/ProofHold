package com.proofhold.claim;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/claims")
public class ClaimController {

    @PostMapping("/{claimId}/decision")
    @PreAuthorize("hasRole('STAFF')")
    public void decide(@PathVariable Long claimId) {
        // Step 7 fills in approve/reject. Role is taken from the JWT, never from the body.
    }
}

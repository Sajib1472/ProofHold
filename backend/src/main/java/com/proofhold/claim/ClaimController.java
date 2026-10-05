package com.proofhold.claim;

import com.proofhold.auth.AuthPrincipals;
import com.proofhold.web.ETags;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/claims")
public class ClaimController {

    private final ClaimService claims;

    public ClaimController(ClaimService claims) {
        this.claims = claims;
    }

    @PostMapping("/{claimId}/decision")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<ClaimDecisionResponse> decide(
            @PathVariable Long claimId,
            @RequestHeader("If-Match") String ifMatch,
            @Valid @RequestBody ClaimDecisionRequest request) {
        ClaimDecisionResponse body = claims.decide(AuthPrincipals.require(), claimId, ifMatch, request);
        return ResponseEntity.ok()
                .header(HttpHeaders.ETAG, ETags.quote(body.itemVersion()))
                .body(body);
    }
}

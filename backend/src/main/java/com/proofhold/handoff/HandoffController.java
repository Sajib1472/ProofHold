package com.proofhold.handoff;

import com.proofhold.auth.AuthPrincipals;
import com.proofhold.web.ETags;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/handoffs")
public class HandoffController {

    private final HandoffService handoffs;

    public HandoffController(HandoffService handoffs) {
        this.handoffs = handoffs;
    }

    @GetMapping("/{handoffId}")
    public HandoffResponse get(@PathVariable Long handoffId) {
        return handoffs.get(AuthPrincipals.require(), handoffId);
    }

    @PostMapping("/{handoffId}/complete")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<HandoffResponse> complete(
            @PathVariable Long handoffId, @RequestHeader("If-Match") String ifMatch) {
        HandoffSubmitResult result = handoffs.complete(AuthPrincipals.require(), handoffId, ifMatch);
        return ResponseEntity.ok()
                .header(HttpHeaders.ETAG, ETags.quote(result.itemVersion()))
                .body(result.handoff());
    }
}

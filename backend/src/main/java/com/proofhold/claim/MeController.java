package com.proofhold.claim;

import com.proofhold.auth.AuthPrincipals;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/me")
public class MeController {

    private final ClaimService claims;

    public MeController(ClaimService claims) {
        this.claims = claims;
    }

    @GetMapping("/claims")
    @PreAuthorize("hasRole('CLAIMER')")
    public ClaimListPage myClaims(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return claims.listMine(AuthPrincipals.require(), page, size);
    }
}

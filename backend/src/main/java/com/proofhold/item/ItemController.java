package com.proofhold.item;

import com.proofhold.auth.AuthPrincipals;
import com.proofhold.domain.ItemCategory;
import com.proofhold.domain.ItemStatus;
import com.proofhold.web.ETags;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/items")
public class ItemController {

    private final ItemService items;
    private final com.proofhold.claim.ClaimService claims;
    private final com.proofhold.handoff.HandoffService handoffs;

    public ItemController(
            ItemService items,
            com.proofhold.claim.ClaimService claims,
            com.proofhold.handoff.HandoffService handoffs) {
        this.items = items;
        this.claims = claims;
        this.handoffs = handoffs;
    }

    @GetMapping
    public PublicItemPage list(
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) ItemCategory category,
            @RequestParam(required = false) ItemStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return items.list(locationId, category, status, q, page, size);
    }

    @PostMapping
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<StaffItemResponse> create(@Valid @RequestBody CreateItemRequest request) {
        StaffItemResponse body = items.create(AuthPrincipals.require(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.LOCATION, "/v1/items/" + body.id())
                .header(HttpHeaders.ETAG, ETags.quote(body.version()))
                .body(body);
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<Object> get(@PathVariable Long itemId) {
        Object body = items.get(itemId, AuthPrincipals.optional());
        int version = versionOf(body);
        return ResponseEntity.ok().header(HttpHeaders.ETAG, ETags.quote(version)).body(body);
    }

    @GetMapping("/{itemId}/challenges")
    @PreAuthorize("hasRole('CLAIMER')")
    public List<ChallengePrompt> challenges(@PathVariable Long itemId) {
        return claims.prompts(itemId);
    }

    @PostMapping("/{itemId}/claims")
    @PreAuthorize("hasRole('CLAIMER')")
    public ResponseEntity<com.proofhold.claim.ClaimResponse> submitClaim(
            @PathVariable Long itemId,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody com.proofhold.claim.CreateClaimRequest request) {
        var result = claims.submit(AuthPrincipals.require(), itemId, idempotencyKey, request);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(status);
        if (result.created()) {
            builder.header(HttpHeaders.LOCATION, "/v1/claims/" + result.claim().id());
        }
        return builder.body(result.claim());
    }

    @PostMapping("/{itemId}/handoffs")
    public ResponseEntity<com.proofhold.handoff.HandoffResponse> createHandoff(
            @PathVariable Long itemId,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @RequestHeader("If-Match") String ifMatch,
            @Valid @RequestBody com.proofhold.handoff.CreateHandoffRequest request) {
        var result = handoffs.create(AuthPrincipals.require(), itemId, idempotencyKey, ifMatch, request);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(status)
                .header(HttpHeaders.ETAG, ETags.quote(result.itemVersion()));
        if (result.created()) {
            builder.header(HttpHeaders.LOCATION, "/v1/handoffs/" + result.handoff().id());
        }
        return builder.body(result.handoff());
    }

    private static int versionOf(Object body) {
        if (body instanceof PublicItemResponse publicItem) {
            return publicItem.version();
        }
        if (body instanceof StaffItemResponse staffItem) {
            return staffItem.version();
        }
        if (body instanceof VerifiedClaimerItemResponse verified) {
            return verified.version();
        }
        return 0;
    }
}

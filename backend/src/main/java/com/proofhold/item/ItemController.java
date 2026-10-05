package com.proofhold.item;

import com.proofhold.auth.AuthPrincipals;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/items")
public class ItemController {

    private final ItemService items;

    public ItemController(ItemService items) {
        this.items = items;
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

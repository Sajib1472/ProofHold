package com.proofhold.item;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/items")
public class ItemController {

    @PostMapping
    @PreAuthorize("hasRole('STAFF')")
    @ResponseStatus(HttpStatus.CREATED)
    public void create() {
        // Step 4 fills in logging an item. Role is taken from the JWT, never from the body.
    }
}

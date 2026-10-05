package com.proofhold.item;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class HoldExpiryJob {

    private final ItemService items;

    public HoldExpiryJob(ItemService items) {
        this.items = items;
    }

    @Scheduled(fixedDelayString = "${proofhold.expiry.interval-ms:60000}")
    public void run() {
        items.expireDue(Instant.now());
    }
}

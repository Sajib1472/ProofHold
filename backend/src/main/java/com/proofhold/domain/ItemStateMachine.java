package com.proofhold.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Single place for item status transitions. Callers map failures to 409 and write an audit event.
 */
public final class ItemStateMachine {

    private static final Map<ItemStatus, Set<ItemStatus>> ALLOWED = new EnumMap<>(ItemStatus.class);

    static {
        ALLOWED.put(ItemStatus.LOGGED, EnumSet.of(ItemStatus.HELD));
        ALLOWED.put(ItemStatus.HELD, EnumSet.of(ItemStatus.CLAIM_PENDING, ItemStatus.EXPIRED));
        ALLOWED.put(ItemStatus.CLAIM_PENDING, EnumSet.of(ItemStatus.VERIFIED, ItemStatus.HELD, ItemStatus.EXPIRED));
        ALLOWED.put(ItemStatus.VERIFIED, EnumSet.of(ItemStatus.READY_FOR_PICKUP, ItemStatus.HELD));
        ALLOWED.put(ItemStatus.READY_FOR_PICKUP, EnumSet.of(ItemStatus.RETURNED, ItemStatus.EXPIRED));
        ALLOWED.put(ItemStatus.EXPIRED, EnumSet.of(ItemStatus.DONATED));
        ALLOWED.put(ItemStatus.RETURNED, EnumSet.noneOf(ItemStatus.class));
        ALLOWED.put(ItemStatus.DONATED, EnumSet.noneOf(ItemStatus.class));
    }

    private ItemStateMachine() {}

    public static boolean canTransition(ItemStatus from, ItemStatus to) {
        Set<ItemStatus> next = ALLOWED.get(from);
        return next != null && next.contains(to);
    }

    public static ItemStatus require(ItemStatus from, ItemStatus to) {
        if (!canTransition(from, to)) {
            throw new IllegalTransitionException(from, to);
        }
        return to;
    }
}

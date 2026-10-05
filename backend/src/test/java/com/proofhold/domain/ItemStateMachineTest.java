package com.proofhold.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ItemStateMachineTest {

    @Test
    void rejectsReturnedToHeld() {
        assertThrows(
                IllegalTransitionException.class,
                () -> ItemStateMachine.require(ItemStatus.RETURNED, ItemStatus.HELD));
    }

    @Test
    void allowsHeldToClaimPending() {
        assertEquals(
                ItemStatus.CLAIM_PENDING,
                ItemStateMachine.require(ItemStatus.HELD, ItemStatus.CLAIM_PENDING));
    }
}

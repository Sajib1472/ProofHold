package com.proofhold.domain;

public class IllegalTransitionException extends RuntimeException {

    private final ItemStatus from;
    private final ItemStatus to;

    public IllegalTransitionException(ItemStatus from, ItemStatus to) {
        super("Illegal transition: " + from + " → " + to);
        this.from = from;
        this.to = to;
    }

    public ItemStatus getFrom() {
        return from;
    }

    public ItemStatus getTo() {
        return to;
    }
}

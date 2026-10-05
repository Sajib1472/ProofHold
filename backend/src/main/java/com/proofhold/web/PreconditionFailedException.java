package com.proofhold.web;

public class PreconditionFailedException extends RuntimeException {

    public PreconditionFailedException(String detail) {
        super(detail);
    }
}

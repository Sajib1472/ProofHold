package com.proofhold.web;

import java.util.List;

public class ValidationFailedException extends RuntimeException {

    private final List<Problem.FieldError> errors;

    public ValidationFailedException(String detail, List<Problem.FieldError> errors) {
        super(detail);
        this.errors = errors;
    }

    public List<Problem.FieldError> getErrors() {
        return errors;
    }
}

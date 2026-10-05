package com.proofhold.web;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Problem(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        List<FieldError> errors) {

    public record FieldError(String field, String code, String message) {}

    public static Problem of(String type, String title, int status, String detail, String instance) {
        return new Problem(type, title, status, detail, instance, null);
    }
}

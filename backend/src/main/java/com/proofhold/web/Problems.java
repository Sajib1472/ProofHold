package com.proofhold.web;

import org.springframework.http.MediaType;

public final class Problems {

    public static final MediaType MEDIA_TYPE = MediaType.parseMediaType("application/problem+json");
    public static final String BASE = "https://proofhold.local/problems/";

    private Problems() {}

    public static Problem unauthorized(String instance) {
        return Problem.of(BASE + "unauthorized", "Unauthorized", 401, "Authentication is required.", instance);
    }

    public static Problem forbidden(String instance, String detail) {
        return Problem.of(BASE + "forbidden", "Forbidden", 403, detail, instance);
    }

    public static Problem conflict(String instance, String detail) {
        return Problem.of(BASE + "conflict", "Conflict", 409, detail, instance);
    }

    public static Problem badRequest(String instance, String detail) {
        return Problem.of(BASE + "bad-request", "Bad request", 400, detail, instance);
    }

    public static Problem validation(String instance, String detail, java.util.List<Problem.FieldError> errors) {
        return new Problem(BASE + "validation", "Validation failed", 422, detail, instance, errors);
    }

    public static Problem notFound(String instance, String detail) {
        return Problem.of(BASE + "not-found", "Not found", 404, detail, instance);
    }

    public static Problem conflict(String slug, String title, String detail, String instance) {
        return Problem.of(BASE + slug, title, 409, detail, instance);
    }

    public static Problem preconditionFailed(String instance, String detail) {
        return Problem.of(BASE + "precondition-failed", "Precondition failed", 412, detail, instance);
    }
}

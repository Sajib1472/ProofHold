package com.proofhold.web;

public class ConflictException extends RuntimeException {

    private final String slug;
    private final String title;

    public ConflictException(String slug, String title, String detail) {
        super(detail);
        this.slug = slug;
        this.title = title;
    }

    public String getSlug() {
        return slug;
    }

    public String getTitle() {
        return title;
    }
}

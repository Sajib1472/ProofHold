package com.proofhold.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public final class ProblemResponses {

    private ProblemResponses() {}

    public static ResponseEntity<Problem> of(Problem problem) {
        return ResponseEntity.status(problem.status()).contentType(Problems.MEDIA_TYPE).body(problem);
    }

    public static ResponseEntity<Problem> status(HttpStatus status, Problem problem) {
        return ResponseEntity.status(status).contentType(Problems.MEDIA_TYPE).body(problem);
    }
}

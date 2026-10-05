package com.proofhold.web;

import com.proofhold.auth.DuplicateEmailException;
import com.proofhold.auth.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(UnauthorizedException.class)
    ResponseEntity<Problem> unauthorized(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .contentType(Problems.MEDIA_TYPE)
                .body(Problems.unauthorized(request.getRequestURI()));
    }

    @ExceptionHandler(DuplicateEmailException.class)
    ResponseEntity<Problem> duplicateEmail(DuplicateEmailException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .contentType(Problems.MEDIA_TYPE)
                .body(Problems.conflict(request.getRequestURI(), ex.getMessage()));
    }
}

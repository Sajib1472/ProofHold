package com.proofhold.web;

import com.proofhold.domain.IllegalTransitionException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class ProblemExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Problem> badRequest(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(Problems.MEDIA_TYPE)
                .body(Problems.badRequest(request.getRequestURI(), "Request body is not valid JSON."));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    ResponseEntity<Problem> missingHeader(MissingRequestHeaderException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(Problems.MEDIA_TYPE)
                .body(Problems.badRequest(request.getRequestURI(), "Missing header: " + ex.getHeaderName()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Problem> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<Problem.FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> new Problem.FieldError(
                        err.getField(),
                        err.getCode() == null ? "invalid" : err.getCode(),
                        err.getDefaultMessage() == null ? "Invalid" : err.getDefaultMessage()))
                .toList();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .contentType(Problems.MEDIA_TYPE)
                .body(Problems.validation(request.getRequestURI(), "Request is not valid.", errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<Problem> constraint(ConstraintViolationException ex, HttpServletRequest request) {
        List<Problem.FieldError> errors = ex.getConstraintViolations().stream()
                .map(v -> new Problem.FieldError(
                        v.getPropertyPath().toString(),
                        v.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName(),
                        v.getMessage()))
                .toList();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .contentType(Problems.MEDIA_TYPE)
                .body(Problems.validation(request.getRequestURI(), "Request is not valid.", errors));
    }

    @ExceptionHandler(ValidationFailedException.class)
    ResponseEntity<Problem> failedValidation(ValidationFailedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .contentType(Problems.MEDIA_TYPE)
                .body(Problems.validation(request.getRequestURI(), ex.getMessage(), ex.getErrors()));
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<Problem> notFound(NotFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(Problems.MEDIA_TYPE)
                .body(Problems.notFound(request.getRequestURI(), ex.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<Problem> conflict(ConflictException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .contentType(Problems.MEDIA_TYPE)
                .body(Problems.conflict(ex.getSlug(), ex.getTitle(), ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(IllegalTransitionException.class)
    ResponseEntity<Problem> illegalTransition(IllegalTransitionException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .contentType(Problems.MEDIA_TYPE)
                .body(Problems.conflict(
                        "illegal-transition",
                        "Illegal transition",
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Problem> denied(AccessDeniedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .contentType(Problems.MEDIA_TYPE)
                .body(Problems.forbidden(request.getRequestURI(), ex.getMessage()));
    }

    @ExceptionHandler(PreconditionFailedException.class)
    ResponseEntity<Problem> precondition(PreconditionFailedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
                .contentType(Problems.MEDIA_TYPE)
                .body(Problems.preconditionFailed(request.getRequestURI(), ex.getMessage()));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<Problem> stale(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
                .contentType(Problems.MEDIA_TYPE)
                .body(Problems.preconditionFailed(request.getRequestURI(), "Item version does not match If-Match."));
    }
}

package com.proofhold.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
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
}

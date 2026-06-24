package com.melina.jobtrail.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(UserNotFoundException ex) {
        return createErrorResponseEntity(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExistsException(EmailAlreadyExistsException ex) {
        return createErrorResponseEntity(HttpStatus.CONFLICT,ex.getMessage());
    }

    @ExceptionHandler(ApplicationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleApplicationNotFoundException(ApplicationNotFoundException ex) {
        return createErrorResponseEntity(HttpStatus.NOT_FOUND,ex.getMessage());
    }

    @ExceptionHandler(CompanyNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCompanyNotFoundException(CompanyNotFoundException ex) {
        return createErrorResponseEntity(HttpStatus.NOT_FOUND,ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException() {
        return createErrorResponseEntity(HttpStatus.CONFLICT,"Data integrity violation");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );
        return createErrorResponseEntity(HttpStatus.BAD_REQUEST,"Validation failed",fieldErrors);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException() {
        return createErrorResponseEntity(HttpStatus.UNAUTHORIZED,"Invalid credentials");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions() {
        return createErrorResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR,"An unexpected error occurred");
    }

    private ResponseEntity<ErrorResponse> createErrorResponseEntity(HttpStatus httpStatus, String message) {
        return createErrorResponseEntity(httpStatus,message,null);
    }

    private ResponseEntity<ErrorResponse> createErrorResponseEntity(
            HttpStatus httpStatus,
            String message,
            Map<String, String> fieldErrors) {
        ErrorResponse errorResponse = new ErrorResponse(
                httpStatus,
                message,
                fieldErrors
        );
        return new ResponseEntity<>(errorResponse, httpStatus);
    }

}

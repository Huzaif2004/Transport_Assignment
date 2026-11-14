package com.transport.assignment.exception;

import com.transport.assignment.dto.response.ApiErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidInput(
            InvalidInputException ex
    ) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage(), List.of());
    }

    @ExceptionHandler(InputDataNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleInputDataNotFound(
            InputDataNotFoundException ex
    ) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), List.of());
    }

    @ExceptionHandler(NoFeasibleAssignmentException.class)
    public ResponseEntity<ApiErrorResponse> handleNoFeasibleAssignment(
            NoFeasibleAssignmentException ex
    ) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), List.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex
    ) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage(), List.of());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalState(
            IllegalStateException ex
    ) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex
    ) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        return error(HttpStatus.BAD_REQUEST, "Validation failed.", errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex
    ) {
        List<String> errors = ex.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();

        return error(HttpStatus.BAD_REQUEST, "Validation failed.", errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableMessage(
            HttpMessageNotReadableException ex
    ) {
        return error(HttpStatus.BAD_REQUEST, "Malformed request body.", List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception ex
    ) {
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred.",
                List.of()
        );
    }

    private ResponseEntity<ApiErrorResponse> error(
            HttpStatus status,
            String message,
            List<String> errors
    ) {
        return ResponseEntity
                .status(status)
                .body(new ApiErrorResponse("error", message, errors));
    }
}

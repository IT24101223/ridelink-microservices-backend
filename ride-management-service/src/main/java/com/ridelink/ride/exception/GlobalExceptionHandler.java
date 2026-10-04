package com.ridelink.ride.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralised error response handling.
 *
 * <p>All responses use the shape:
 * <pre>
 * {
 *   "timestamp": "...",
 *   "status": 409,
 *   "error": "Conflict",
 *   "message": "Transition from ACCEPTED to REQUESTED is not allowed.",
 *   "path": "/api/rides/abc/accept",
 *   "details": [...]  // present for validation errors
 * }
 * </pre>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── 404 ────────────────────────────────────────────────────────────
    @ExceptionHandler(RideNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            RideNotFoundException ex, HttpServletRequest req) {
        return body(HttpStatus.NOT_FOUND, ex.getMessage(), req, null);
    }

    // ── 409 – bad state transition ──────────────────────────────────────
    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidTransition(
            InvalidStatusTransitionException ex, HttpServletRequest req) {
        return body(HttpStatus.CONFLICT, ex.getMessage(), req, null);
    }

    // ── 409 – no driver ────────────────────────────────────────────────
    @ExceptionHandler(NoDriverAvailableException.class)
    public ResponseEntity<Map<String, Object>> handleNoDriver(
            NoDriverAvailableException ex, HttpServletRequest req) {
        return body(HttpStatus.CONFLICT, ex.getMessage(), req, null);
    }

    // ── 403 – ownership ────────────────────────────────────────────────
    @ExceptionHandler({
            com.ridelink.ride.exception.AccessDeniedException.class,
            AccessDeniedException.class
    })
    public ResponseEntity<Map<String, Object>> handleForbidden(
            RuntimeException ex, HttpServletRequest req) {
        return body(HttpStatus.FORBIDDEN, ex.getMessage(), req, null);
    }

    // ── 400 – bean validation (@Valid on request body) ──────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());
        return body(HttpStatus.BAD_REQUEST, "Validation failed", req, details);
    }

    // ── 400 – constraint violation (path / query params) ────────────────
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraint(
            ConstraintViolationException ex, HttpServletRequest req) {
        List<String> details = ex.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .collect(Collectors.toList());
        return body(HttpStatus.BAD_REQUEST, "Constraint violation", req, details);
    }

    // ── 502/503 – downstream failures ───────────────────────────────────
    @ExceptionHandler(DownstreamServiceException.class)
    public ResponseEntity<Map<String, Object>> handleDownstream(
            DownstreamServiceException ex, HttpServletRequest req) {
        log.error("Downstream service error – {}", ex.getMessage(), ex);
        return body(HttpStatus.BAD_GATEWAY, ex.getMessage(), req, null);
    }

    // ── 500 – catch-all ─────────────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(
            Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception", ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.", req, null);
    }

    // ── Helper ──────────────────────────────────────────────────────────
    private ResponseEntity<Map<String, Object>> body(
            HttpStatus status, String message,
            HttpServletRequest req, List<String> details) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", req.getRequestURI());
        if (details != null && !details.isEmpty()) {
            body.put("details", details);
        }
        return ResponseEntity.status(status).body(body);
    }
}

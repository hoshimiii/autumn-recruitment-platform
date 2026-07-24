package dev.recruitops.web;

import dev.recruitops.infrastructure.MissingResourceException;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(MissingResourceException.class) ResponseEntity<Object> missing(MissingResourceException error) { return response(HttpStatus.NOT_FOUND, "not_found", error.getMessage(), Map.of()); }
  @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class}) ResponseEntity<Object> validation(Exception error) {
    Map<String, String> details = error instanceof MethodArgumentNotValidException invalid ? invalid.getBindingResult().getFieldErrors().stream().collect(Collectors.toMap(e -> e.getField(), e -> e.getDefaultMessage(), (a,b) -> a)) : Map.of();
    return response(HttpStatus.BAD_REQUEST, "validation_error", error.getMessage() == null ? "Invalid request" : error.getMessage(), details);
  }
  @ExceptionHandler(IllegalStateException.class) ResponseEntity<Object> conflict(IllegalStateException error) { return response(HttpStatus.CONFLICT, "invalid_state", error.getMessage(), Map.of()); }
  private ResponseEntity<Object> response(HttpStatus status, String code, String message, Map<?, ?> details) {
    return ResponseEntity.status(status).body(Map.of("error", Map.of("code", code, "message", message, "details", details)));
  }
}

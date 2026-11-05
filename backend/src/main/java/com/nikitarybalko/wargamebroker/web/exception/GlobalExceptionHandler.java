package com.nikitarybalko.wargamebroker.web.exception;

import com.nikitarybalko.wargamebroker.domain.exception.InvalidLifecycleTransitionException;
import com.nikitarybalko.wargamebroker.domain.exception.InvalidTickException;
import com.nikitarybalko.wargamebroker.domain.exception.SimulationException;
import com.nikitarybalko.wargamebroker.domain.exception.SimulationNotActiveException;
import com.nikitarybalko.wargamebroker.domain.exception.SimulatorNotRegisteredException;
import com.nikitarybalko.wargamebroker.domain.exception.SnapshotNotFoundException;
import com.nikitarybalko.wargamebroker.domain.exception.TickDataUnavailableException;
import com.nikitarybalko.wargamebroker.domain.exception.TickNotCompleteException;
import com.nikitarybalko.wargamebroker.domain.model.EventType;
import com.nikitarybalko.wargamebroker.domain.service.StateCheckpointingService;
import com.nikitarybalko.wargamebroker.web.dto.ErrorResponse;
import com.nikitarybalko.wargamebroker.web.security.ForbiddenException;
import com.nikitarybalko.wargamebroker.web.security.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final StateCheckpointingService checkpointingService;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .toList();

        String message = "Validation failed for request payload";
        checkpointingService.recordEvent(EventType.VALIDATION_ERROR, null, message + ": " + String.join(", ", details));
        log.warn("Validation error: {}", details);

        return buildResponse(HttpStatus.BAD_REQUEST, message, details);
    }

    @ExceptionHandler(InvalidTickException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTickException(InvalidTickException ex) {
        log.warn("Invalid tick submission: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), Collections.emptyList());
    }

    @ExceptionHandler({
            SimulationNotActiveException.class,
            SimulatorNotRegisteredException.class,
            InvalidLifecycleTransitionException.class,
            TickNotCompleteException.class
    })
    public ResponseEntity<ErrorResponse> handleConflictException(SimulationException ex) {
        log.warn("Simulation state conflict: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), Collections.emptyList());
    }

    @ExceptionHandler({SnapshotNotFoundException.class, TickDataUnavailableException.class})
    public ResponseEntity<ErrorResponse> handleNotFoundException(SimulationException ex) {
        log.warn("Requested resource not found: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), Collections.emptyList());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenException(ForbiddenException ex) {
        log.warn("Forbidden access attempt: {}", ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), Collections.emptyList());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(UnauthorizedException ex) {
        log.warn("Unauthorized access: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), Collections.emptyList());
    }

    @ExceptionHandler({SimulationException.class, IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ErrorResponse> handleBusinessExceptions(RuntimeException ex) {
        log.warn("Client business error: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), Collections.emptyList());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        log.error("Internal system error occurred", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected server error occurred", Collections.emptyList());
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message, List<String> details) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .details(details)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}

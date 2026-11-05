package com.nikitarybalko.wargamebroker.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Standard API error response representation")
public class ErrorResponse {

    @Schema(description = "Timestamp when error occurred")
    private Instant timestamp;

    @Schema(description = "HTTP status code", example = "400")
    private int status;

    @Schema(description = "Error type description", example = "Bad Request")
    private String error;

    @Schema(description = "Main error message", example = "Invalid tick number requested")
    private String message;

    @Schema(description = "Detailed validation or contextual error items")
    private List<String> details;
}

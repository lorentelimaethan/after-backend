package com.afterApp.after.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Standard API error response returned by the global exception handler")
public class ErrorResponseDTO {
    @Schema(example = "400", description = "HTTP status code")
    private int status;

    @Schema(example = "Bad Request", description = "HTTP reason phrase")
    private String error;

    @Schema(example = "Event capacity is full", description = "Human-readable error message")
    private String message;

    @Schema(example = "/events/6/join", description = "Request path that produced the error")
    private String path;

    @Schema(example = "2026-08-11T13:25:00", description = "Time when the error response was generated")
    private LocalDateTime timestamp;
}

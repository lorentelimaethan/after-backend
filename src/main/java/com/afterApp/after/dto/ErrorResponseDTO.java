package com.afterApp.after.dto;

import lombok.*;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponseDTO {
    private int status;
    private String error;
    private String message;
    private String path;
    private LocalDateTime timestamp;
}

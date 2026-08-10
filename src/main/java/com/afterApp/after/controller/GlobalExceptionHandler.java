package com.afterApp.after.controller;

import com.afterApp.after.dto.ErrorResponseDTO;
import com.afterApp.after.exceptions.BadRequestException;
import com.afterApp.after.exceptions.FormatRequestException;
import com.afterApp.after.exceptions.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException exception){
        Map<String, String> errors = new HashMap<>();

        exception.getBindingResult().getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidJson(
            HttpMessageNotReadableException exception, HttpServletRequest request) {
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Invalid request body")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponseDTO> handleInternalServerError(RuntimeException exception, HttpServletRequest request){
       ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
               .timestamp(LocalDateTime.now())
               .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
               .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
               .message("Unexpected Server Error")
               .path(request.getRequestURI())
               .build();

       return ResponseEntity.internalServerError().body(errorResponse);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotFound(NotFoundException exception, HttpServletRequest request){
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message("Content not Found")
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler({
            BadRequestException.class,
            FormatRequestException.class
    })
    public ResponseEntity<ErrorResponseDTO> handleBadRequest(RuntimeException exception, HttpServletRequest request){
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(exception.getMessage())
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }


}

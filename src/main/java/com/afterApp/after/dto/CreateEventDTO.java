package com.afterApp.after.dto;

import com.afterApp.after.entity.Address;
import com.afterApp.after.enums.EventType;
import com.afterApp.after.enums.MusicStyle;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CreateEventDTO {

    @NotBlank(message = "Name required")
    private String name;

    @NotNull(message = "Event must have a type")
    private EventType eventType;

    @NotNull(message = "Event must have a music Style")
    private MusicStyle musicStyle;

    @NotBlank(message = "Event must have a description")
    private String description;

    @NotNull(message = "Event must have a date and time")
    @Future(message = "Event date and time must be in the future")
    private LocalDateTime dateTime;

    @Positive(message = "Event capacity must be > 0")
    private int capacity;

    @Valid
    @NotNull(message = "Event must have and address")
    private Address address;
}

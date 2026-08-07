package com.afterApp.after.dto;

import com.afterApp.after.enums.EventType;
import com.afterApp.after.enums.MusicStyle;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UpdateEventDTO {

    private String name;

    private EventType eventType;

    private MusicStyle musicStyle;

    private String description;

    @Future(message = "Event date and time must be in the future")
    private LocalDateTime dateTime;

    @Positive(message = "Event capacity must be > 0")
    private Integer capacity;

    @Valid
    private AddressDTO address;
}

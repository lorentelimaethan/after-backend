package com.afterApp.after.dto;

import com.afterApp.after.enums.EventType;
import com.afterApp.after.enums.MusicStyle;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class EventResponseDTO {

    private Long id;

    private String name;

    private String description;

    private LocalDateTime dateTime;

    private int capacity;

    private EventType eventType;

    private MusicStyle musicStyle;

    private String hostDisplayName;

    private int usersCount;
}

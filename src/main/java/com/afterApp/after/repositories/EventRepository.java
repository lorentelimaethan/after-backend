package com.afterApp.after.repositories;

import com.afterApp.after.entity.Events;
import com.afterApp.after.enums.EventType;
import com.afterApp.after.enums.MusicStyle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Events, Long> {
    List<Events> findByEventType(EventType Type);
    List<Events> findByMusicStyle(MusicStyle style);
    List<Events> findByEventTypeAndMusicStyle(EventType eventType, MusicStyle musicStyle);;
}

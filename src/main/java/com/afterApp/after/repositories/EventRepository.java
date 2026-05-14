package com.afterApp.after.repositories;

import com.afterApp.after.entity.Event;
import com.afterApp.after.enums.EventType;
import com.afterApp.after.enums.MusicStyle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByEventType(EventType Type);
    List<Event> findByMusicStyle(MusicStyle style);
    List<Event> findByEventTypeAndMusicStyle(EventType eventType, MusicStyle musicStyle);;
}

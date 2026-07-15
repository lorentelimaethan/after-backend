package com.afterApp.after.loader;

import com.afterApp.after.entity.Events;
import com.afterApp.after.enums.EventType;
import com.afterApp.after.enums.MusicStyle;
import com.afterApp.after.exceptions.NotFoundException;
import com.afterApp.after.repositories.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EventLoader {
    @Autowired
    private EventRepository eventRepository;

    public List<Events> getAllEvents(){
        return eventRepository.findAll();
    }

    public Events getEventById(Long id){
        return eventRepository.findById(id).orElseThrow(() -> new NotFoundException("Event not found"));
    }

    public List<Events> getEventsByType(EventType type){
        return eventRepository.findByEventType(type);
    }

    public List<Events> getEventsByMusicStyle(MusicStyle style){
        return eventRepository.findByMusicStyle(style);
    }

    public List<Events> getEventsByTypeAndStyle(EventType type, MusicStyle style){
        return eventRepository.findByEventTypeAndMusicStyle(type, style);
    }

    public Events saveEvent(Events e){
        return eventRepository.save(e);
    }

    public void deleteEvent(Events e){
        eventRepository.delete(e);
    }
}

package com.afterApp.after.service;

import com.afterApp.after.entity.Event;
import com.afterApp.after.entity.User;
import com.afterApp.after.entity.UserAccess;
import com.afterApp.after.enums.EventType;
import com.afterApp.after.enums.MusicStyle;
import com.afterApp.after.exceptions.BadRequestException;
import com.afterApp.after.exceptions.NotFoundException;
import com.afterApp.after.exceptions.UnauthorizedException;
import com.afterApp.after.repositories.EventRepository;
import com.afterApp.after.repositories.UserAccessRepository;
import com.afterApp.after.repositories.UserRepository;
import com.afterApp.after.utils.TokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventServices {
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private TokenUtil tokenUtil;
    @Autowired
    private UserAccessRepository userAccessRepository;
    @Autowired
    private UserRepository userRepository;

    public List<Event> getAllEvents() { return eventRepository.findAll(); }

    public Event getEvent(Long id) throws RuntimeException{
        return eventRepository.findById(id).orElseThrow(() -> new NotFoundException("Event not found"));
    }

    public List<Event> getEventsByType(EventType type){
        return eventRepository.findByEventType(type);
    }
    public List<Event> getEventsByStyle(MusicStyle style){ return eventRepository.findByMusicStyle(style); }
    public List<Event> getEventsByTypeAndStyle(EventType type, MusicStyle style){
        return eventRepository.findByEventTypeAndMusicStyle(type, style);
    }

    private User extractUser(String authorization){
        String jwt = authorization.replace("Bearer ", "");
        String username = tokenUtil.extractUsername(jwt);

        UserAccess userAccess = userAccessRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not Found"));

        return userAccess.getUser();
    }

    public Event createEvent(Event e, String authorization){
        if(e.getCapacity() <= 0){
            throw new RuntimeException("Capacity must be > 0");
        }

        if(e.getName() == null || e.getName().isBlank()){
            throw new RuntimeException("Name required");
        }


        User host = extractUser(authorization);

        e.setHost(host);

        return eventRepository.save(e);
    }

    public Event joinEvent(String authorization, Long id){
        User requester = extractUser(authorization);

        Event e = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        if(e.getUsers().size() >= e.getCapacity()){
            throw new BadRequestException("Event capacity is full");
        }

        e.getUsers().add(requester);
        return eventRepository.save(e);
    }

    public Event leaveEvent(String authorization, Long id){
        User requester = extractUser(authorization);

        Event e = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        e.getUsers().remove(requester);
        return eventRepository.save(e);
    }

    public Event kickUser(String authorization, Long eventId, Long userId) {
        User requester = extractUser(authorization);

        Event e = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        if(!e.getHost().getId().equals(requester.getId())){
            throw new UnauthorizedException("Only host can delete Users");
        }

        User userToKick = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if(!e.getUsers().contains(userToKick)){
            throw new NotFoundException("User is not in the Event");
        }

        if(e.getHost().getId().equals(userToKick.getId())){
            throw new BadRequestException("Host can not be kicked");
        }

        e.getUsers().remove(userToKick);

        return eventRepository.save(e);
    }

    public Event inviteUser(String authorization, Long eventId, Long userId){
        User requester = extractUser(authorization);

        Event e = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        if(!e.getHost().getId().equals(requester.getId())){
            throw new UnauthorizedException("Only host can delete Users");
        }

        User userToInvite = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if(e.getUsers().contains(userToInvite)){
            throw new NotFoundException("User is not in the Event");
        }

        if(e.getHost().getId().equals(userToInvite.getId())){
            throw new BadRequestException("Host already in the event");
        }

        e.getUsers().add(userToInvite);

        return eventRepository.save(e);
    }

    public void deleteEvent(Long id, String authorization) throws RuntimeException{
        User user = extractUser(authorization);

        Event e = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        if(!e.getHost().getId().equals(user.getId())){
            throw new UnauthorizedException("Only host can add Users");
        }

        eventRepository.delete(e);
    }
}

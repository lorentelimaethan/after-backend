package com.afterApp.after.service;

import com.afterApp.after.dto.CreateEventDTO;
import com.afterApp.after.dto.EventResponseDTO;
import com.afterApp.after.dto.UpdateEventDTO;
import com.afterApp.after.entity.Address;
import com.afterApp.after.entity.Events;
import com.afterApp.after.entity.Users;
import com.afterApp.after.entity.UserAccess;
import com.afterApp.after.enums.EventType;
import com.afterApp.after.enums.MusicStyle;
import com.afterApp.after.exceptions.BadRequestException;
import com.afterApp.after.exceptions.NotFoundException;
import com.afterApp.after.exceptions.UnauthorizedException;
import com.afterApp.after.loader.EventLoader;
import com.afterApp.after.mappers.EventMapper;
import com.afterApp.after.repositories.EventRepository;
import com.afterApp.after.repositories.UserAccessRepository;
import com.afterApp.after.repositories.UserRepository;
import com.afterApp.after.utils.TokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.afterApp.after.mappers.EventMapper.toDto;
import static com.afterApp.after.mappers.EventMapper.updateEventData;

@Service
public class EventServices {
    @Autowired
    private TokenUtil tokenUtil;
    @Autowired
    private UserAccessRepository userAccessRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EventLoader eventLoader;


    public List<EventResponseDTO> getAllEvents() {
        List<Events> events = eventLoader.getAllEvents();

        return events.stream().map(EventMapper::toDto).toList();
    }

    public EventResponseDTO getEvent(Long id) throws RuntimeException{
        Events e = eventLoader.getEventById(id);

        return toDto(e);
    }

    public List<EventResponseDTO> getEventsByType(EventType type){
        List<Events> events = eventLoader.getEventsByType(type);

        return events.stream().map(EventMapper::toDto).toList();
    }

    public List<EventResponseDTO> getEventsByStyle(MusicStyle style){
        List<Events> events = eventLoader.getEventsByMusicStyle(style);

        return  events.stream().map(EventMapper::toDto).toList();
    }

    public List<EventResponseDTO> getEventsByTypeAndStyle(EventType type, MusicStyle style){
        List<Events> events = eventLoader.getEventsByTypeAndStyle(type, style);

        return events.stream().map(EventMapper::toDto).toList();
    }

    private Users extractUser(String authorization){
        String username = tokenUtil.extractUsername(authorization);

        UserAccess userAccess = userAccessRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not Found"));

        return userAccess.getUser();
    }

    public EventResponseDTO createEvent(CreateEventDTO dto, String authorization){
        Users host = extractUser(authorization);

        Events e = EventMapper.fromDto(dto);

        e.setHost(host);

       return toDto(eventLoader.saveEvent(e));
    }

    public EventResponseDTO joinEvent(String authorization, Long id){
        Users requester = extractUser(authorization);

        Events e = eventLoader.getEventById(id);

        if (e.getHost().getId().equals(requester.getId())) {
            throw new BadRequestException("Host cannot join own event");
        }

        if(e.getUsers().size() >= e.getCapacity()){
            throw new BadRequestException("Event capacity is full");
        }

        boolean alreadyJoined = e.getUsers().stream()
                        .anyMatch(u -> u.getId().equals(requester.getId()));

        if(alreadyJoined){
            throw new BadRequestException("User already joined");
        }

        e.getUsers().add(requester);
        return toDto(eventLoader.saveEvent(e));
    }

    public EventResponseDTO leaveEvent(String authorization, Long id){
        Users requester = extractUser(authorization);

        Events e = eventLoader.getEventById(id);


        if (e.getHost().getId().equals(requester.getId())) {
            throw new BadRequestException("Host cannot leave own event");
        }

        boolean isInEvent = e.getUsers().stream()
                        .anyMatch(u -> u.getId().equals(requester.getId()));

        if (!isInEvent){
            throw new NotFoundException("User not in event");
        }

        e.getUsers().removeIf(u -> u.getId().equals(requester.getId()));
        return toDto(eventLoader.saveEvent(e));
    }

    public EventResponseDTO kickUser(String authorization, Long eventId, Long userId) {
        Users requester = extractUser(authorization);

        Events e = eventLoader.getEventById(eventId);

        if(!e.getHost().getId().equals(requester.getId())){
            throw new UnauthorizedException("Only host can delete Users");
        }

        Users userToKick = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if(e.getHost().getId().equals(userToKick.getId())){
            throw new BadRequestException("Host can not be kicked");
        }

        boolean isInEvent = e.getUsers().stream()
                .anyMatch(u -> u.getId().equals(userToKick.getId()));

        if(!isInEvent){
            throw new NotFoundException("User is not in the Event");
        }

        e.getUsers().removeIf(u -> u.getId().equals(userToKick.getId()));

        return toDto(eventLoader.saveEvent(e));
    }

    public EventResponseDTO inviteUser(String authorization, Long eventId, Long userId){
        Users requester = extractUser(authorization);

        Events e = eventLoader.getEventById(eventId);

        if(!e.getHost().getId().equals(requester.getId())){
            throw new UnauthorizedException("Only host can invite Users");
        }

        Users userToInvite = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        boolean isInEvent = e.getUsers().stream()
                .anyMatch(u -> u.getId().equals(userToInvite.getId()));

            if(isInEvent){
            throw new BadRequestException("User is already in the Event");
        }

        if(e.getHost().getId().equals(userToInvite.getId())){
            throw new BadRequestException("Host already in the event");
        }

        if(e.getUsers().size() >= e.getCapacity()){
            throw new BadRequestException("Event capacity is full");
        }

        e.getUsers().add(userToInvite);

        return toDto(eventLoader.saveEvent(e));
    }

    public void deleteEvent(Long id, String authorization) throws RuntimeException{
        Users user = extractUser(authorization);

        Events e = eventLoader.getEventById(id);

        if(!e.getHost().getId().equals(user.getId())){
            throw new UnauthorizedException("Only host can add Users");
        }

        eventLoader.deleteEvent(e);
    }

    public EventResponseDTO updateEvent(String authorization, Long eventId, UpdateEventDTO eventDTO){
        Users requester = extractUser(authorization);

        Events e = eventLoader.getEventById(eventId);

        if(!e.getHost().getId().equals(requester.getId())){
            throw new UnauthorizedException("Only hosts can update Events");
        }

        if(eventDTO.getCapacity() != null && eventDTO.getCapacity() < e.getUsers().size()){
            throw new BadRequestException("Event capacity cannot be lower than current attendees");
        }

        updateEventData(e, eventDTO);

        return toDto(eventLoader.saveEvent(e));
    }
}

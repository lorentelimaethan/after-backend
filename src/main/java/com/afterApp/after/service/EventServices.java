package com.afterApp.after.service;

import com.afterApp.after.dto.CreateEventDTO;
import com.afterApp.after.dto.EventResponseDTO;
import com.afterApp.after.entity.Address;
import com.afterApp.after.entity.Events;
import com.afterApp.after.entity.Users;
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

    /**
     * Cuando escales:
     *
     * usersPreview -- ver 2 o 3 ususarios en la fiesta en getEventById
     * endpoints separados por permisos, host por ejemplo puede ver que usuarios se unen: GET /events/{id}/admin
     * DTOs por nivel (public/private/admin)
     * decidir como y cuando se ven los usuarios en una fiesta.
     * lógica de visibilidad avanzada
     * Host decide si Usuario se puede unir o no (hacer public/private events o no?)
     */

    public EventResponseDTO toDto(Events e){
        EventResponseDTO dto = new EventResponseDTO();

        dto.setId(e.getId());
        dto.setName(e.getName());
        dto.setDescription(e.getDescription());
        dto.setDateTime(e.getDateTime());
        dto.setCapacity(e.getCapacity());
        dto.setEventType(e.getEventType());
        dto.setMusicStyle(e.getMusicStyle());
        dto.setHostDisplayName(e.getHost().getDisplayName());
        dto.setUsersCount(e.getUsers().size());

        return dto;
    }

    public List<EventResponseDTO> getAllEvents() {
        List<Events> events = eventRepository.findAll();

        return events.stream().map(this::toDto).toList();
    }

    public EventResponseDTO getEvent(Long id) throws RuntimeException{
        Events e = eventRepository.findById(id).orElseThrow(() -> new NotFoundException("Event not found"));

        return toDto(e);
    }

    public List<EventResponseDTO> getEventsByType(EventType type){
        List<Events> events = eventRepository.findByEventType(type);

        return events.stream().map(this::toDto).toList();
    }

    public List<EventResponseDTO> getEventsByStyle(MusicStyle style){
        List<Events> events = eventRepository.findByMusicStyle(style);

        return  events.stream().map(this::toDto).toList();
    }

    public List<EventResponseDTO> getEventsByTypeAndStyle(EventType type, MusicStyle style){
        List<Events> events = eventRepository.findByEventTypeAndMusicStyle(type, style);

        return events.stream().map(this::toDto).toList();
    }

    private Users extractUser(String authorization){
        String jwt = authorization.replace("Bearer ", "");
        String username = tokenUtil.extractUsername(jwt);

        UserAccess userAccess = userAccessRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not Found"));

        return userAccess.getUser();
    }

    public EventResponseDTO createEvent(CreateEventDTO dto, String authorization){
        Users host = extractUser(authorization);

        Events e = new Events();

        e.setEventType(dto.getEventType());
        e.setCapacity(dto.getCapacity());
        e.setName(dto.getName());
        e.setDescription(dto.getDescription());
        e.setDateTime(dto.getDateTime());
        e.setMusicStyle(dto.getMusicStyle());

        Address address = new Address();
        address.setStreetNum(dto.getAddress().getStreetNum());
        address.setStreet(dto.getAddress().getStreet());
        address.setCity(dto.getAddress().getCity());
        address.setProvince(dto.getAddress().getProvince());
        address.setPostalCode(dto.getAddress().getPostalCode());
        address.setAditionalInfo(dto.getAddress().getAdditionalInfo());

        e.setAddress(address);

        e.setHost(host);

       return toDto(eventRepository.save(e));
    }

    public EventResponseDTO joinEvent(String authorization, Long id){
        Users requester = extractUser(authorization);

        Events e = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found"));

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
        return toDto(eventRepository.save(e));
    }

    public EventResponseDTO leaveEvent(String authorization, Long id){
        Users requester = extractUser(authorization);

        Events e = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        if (e.getHost().getId().equals(requester.getId())) {
            throw new BadRequestException("Host cannot leave own event");
        }

        boolean isInEvent = e.getUsers().stream()
                        .anyMatch(u -> u.getId().equals(requester.getId()));

        if (!isInEvent){
            throw new NotFoundException("User not in event");
        }

        e.getUsers().removeIf(u -> u.getId().equals(requester.getId()));
        return toDto(eventRepository.save(e));
    }

    public EventResponseDTO kickUser(String authorization, Long eventId, Long userId) {
        Users requester = extractUser(authorization);

        Events e = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

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

        return toDto(eventRepository.save(e));
    }

    public EventResponseDTO inviteUser(String authorization, Long eventId, Long userId){
        Users requester = extractUser(authorization);

        Events e = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

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

        return toDto(eventRepository.save(e));
    }

    public void deleteEvent(Long id, String authorization) throws RuntimeException{
        Users user = extractUser(authorization);

        Events e = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        if(!e.getHost().getId().equals(user.getId())){
            throw new UnauthorizedException("Only host can add Users");
        }

        eventRepository.delete(e);
    }
}


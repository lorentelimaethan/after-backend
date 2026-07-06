package com.afterApp.after.mappers;

import com.afterApp.after.dto.CreateEventDTO;
import com.afterApp.after.dto.EventResponseDTO;
import com.afterApp.after.entity.Address;
import com.afterApp.after.entity.Events;

public class EventMapper {
    public static EventResponseDTO toDto(Events e){
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

    public static Events fromDto(CreateEventDTO dto){
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

        return e;
    }
}

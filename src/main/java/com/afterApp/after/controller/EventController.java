package com.afterApp.after.controller;

import com.afterApp.after.entity.Event;
import com.afterApp.after.enums.EventType;
import com.afterApp.after.enums.MusicStyle;
import com.afterApp.after.exceptions.BadRequestException;
import com.afterApp.after.exceptions.NotFoundException;
import com.afterApp.after.exceptions.UnauthorizedException;
import com.afterApp.after.service.EventServices;
import com.afterApp.after.utils.TokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/event")
public class EventController {
    @Autowired
    private EventServices eventServices;

    @Autowired
    private TokenUtil tokenUtil;

    @GetMapping
    public ResponseEntity<?> getAllEvents(
            @RequestHeader String authorization,
            @RequestParam(required = false)EventType type,
            @RequestParam(required = false)MusicStyle style
    ){
        Boolean token = tokenUtil.validateToken(authorization);

        if(!token){
            return ResponseEntity.status(401).body("Access denied");
        }

        try{
            List<Event> event;

            if(type != null && style != null){
                event = eventServices.getEventsByTypeAndStyle(type, style);

            } else if (type != null) {
                event = eventServices.getEventsByType(type);

            } else if(style != null){
                event = eventServices.getEventsByStyle(style);

            }
            else {
                event = eventServices.getAllEvents();
            }

            return ResponseEntity.ok(event);

        }catch (RuntimeException e){
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getEventById(@PathVariable Long id, @RequestHeader String authorization){
        Boolean token = tokenUtil.validateToken(authorization);

        if(!token){
            return ResponseEntity.status(401).body("Access denied");
        }

        try {
            return ResponseEntity.ok(eventServices.getEvent(id));
        }catch (NotFoundException e){
            return ResponseEntity.notFound().build();
        }catch (RuntimeException e){
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }


    @PostMapping
    public ResponseEntity<?> createEvent(@RequestBody Event e, @RequestHeader String authorization){
        Boolean token = tokenUtil.validateToken(authorization);

        if(!token){
            return ResponseEntity.status(401).body("Acces denied");
        }

        try{
            return ResponseEntity.ok(eventServices.createEvent(e, authorization));
        }catch (NotFoundException exception){
            return ResponseEntity.notFound().build();
        }catch (RuntimeException exception){
            return ResponseEntity.internalServerError().body(exception.getMessage());
        }
    }

    @PatchMapping("/{id}/join")
    public ResponseEntity<?> JoinEvent(@PathVariable Long id, @RequestHeader String authorization){
        Boolean token = tokenUtil.validateToken(authorization);

        if(!token){
            return ResponseEntity.status(401).body("Acces denied");
        }

        try{
            return ResponseEntity.ok(eventServices.joinEvent(authorization, id));
        }catch (NotFoundException exception){
            return ResponseEntity.notFound().build();
        }catch (RuntimeException exception){
            return ResponseEntity.internalServerError().body(exception.getMessage());
        }
    }

    @PatchMapping("/{id}/leave")
    public ResponseEntity<?> leaveEvent(@PathVariable Long id, @RequestHeader String authorization){
        Boolean token = tokenUtil.validateToken(authorization);

        if(!token){
            return ResponseEntity.status(401).body("Acces denied");
        }

        try{
            return ResponseEntity.ok(eventServices.leaveEvent(authorization, id));
        }catch (NotFoundException exception){
            return ResponseEntity.notFound().build();
        }catch (RuntimeException exception){
            return ResponseEntity.internalServerError().body(exception.getMessage());
        }
    }

    @PatchMapping("/{eventId}/invite/user/{userId}")
    public ResponseEntity<?> inviteUser(@PathVariable Long eventId, @PathVariable Long userId, @RequestHeader String authorization){
        Boolean token = tokenUtil.validateToken(authorization);

        if(!token){
            return ResponseEntity.status(401).body("Acces denied");
        }

        try{
            return ResponseEntity.ok(eventServices.inviteUser(authorization, eventId, userId));
        }catch (UnauthorizedException | BadRequestException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }catch (NotFoundException e){
            return ResponseEntity.notFound().build();
        }catch (RuntimeException e){
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @DeleteMapping("/{eventId}/kick/user/{userId}")
    public ResponseEntity<?> kickUser(@PathVariable Long eventId, @PathVariable Long userId, @RequestHeader String authorization ){
        Boolean token = tokenUtil.validateToken(authorization);

        if(!token){
            return ResponseEntity.status(401).body("Acces denied");
        }

        try{
            return ResponseEntity.ok(eventServices.kickUser(authorization, eventId, userId));
        }catch (UnauthorizedException | BadRequestException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }catch (NotFoundException e){
            return ResponseEntity.notFound().build();
        }catch (RuntimeException e){
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEvent(@PathVariable Long id, @RequestHeader String authorization){
        Boolean token = tokenUtil.validateToken(authorization);

        if(!token){
            return ResponseEntity.status(401).body("Access denied");
        }

        try{
            eventServices.deleteEvent(id, authorization);
            return ResponseEntity.noContent().build();
        }catch (UnauthorizedException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }catch (NotFoundException e){
            return ResponseEntity.notFound().build();
        }catch (RuntimeException e){
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }




    //Gestionar exceptions y docs



}

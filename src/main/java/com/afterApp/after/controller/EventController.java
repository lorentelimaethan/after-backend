package com.afterApp.after.controller;

import com.afterApp.after.dto.CreateEventDTO;
import com.afterApp.after.entity.Events;
import com.afterApp.after.enums.EventType;
import com.afterApp.after.enums.MusicStyle;
import com.afterApp.after.exceptions.BadRequestException;
import com.afterApp.after.exceptions.NotFoundException;
import com.afterApp.after.exceptions.UnauthorizedException;
import com.afterApp.after.service.EventServices;
import com.afterApp.after.utils.TokenUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
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
    @Operation(summary = "Get all Events")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "All events return",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Response Example",
                                    value =  """
                                        [
                                            {
                                                "address": {
                                                    "aditionalInfo": "Industrial warehouse near the beach",
                                                    "city": "Barcelona",
                                                    "id": 4,
                                                    "postalCode": "08005",
                                                    "province": "Catalonia",
                                                    "street": "Carrer Marina",
                                                    "streetNum": "25"
                                                },
                                                "capacity": 150,
                                                "dateTime": "2026-08-15T23:00:00",
                                                "description": "Underground techno party in Barcelona",
                                                "eventType": "CHILL",
                                                "host": {
                                                    "displayName": "ethanlo2",
                                                    "email": "ethanlo@gmail.com",
                                                    "id": 7,
                                                    "lastname": "Lorente",
                                                    "name": "Ethan",
                                                    "phoneNumber": "+34111111222"
                                                },
                                                "id": 6,
                                                "musicStyle": "HOUSE",
                                                "name": "Techno Underground Barcelona",
                                                "users": [
                                                    {
                                                        "displayName": "EthanLorente",
                                                        "email": "admin@gmail.com",
                                                        "id": 8,
                                                        "lastname": "Lorente",
                                                        "name": "Ethan",
                                                        "phoneNumber": "111111111"
                                                    }
                                                ]
                                            }
                                        ]
                                        """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401",
                    description = "Access Denied",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Response Example",
                                    value = """
                                        {
                                            "timestamp": "2026-05-05T12:00:00",
                                            "status": 401,
                                            "error": "Access Denied",
                                            "message": "Acceso Denegado",
                                            "path": "/contactos"
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Response Example",
                                    value = """
                                            {
                                                "timestamp": "2026-05-05T12:00:00",
                                                "status": 500,
                                                "error": "InternalServerError",
                                                "message": "Unexpected server error",
                                                "path": "/contactos"
                                            }
                                            """
                            )
                    )
            )
    })
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
            List<Events> events;

            if(type != null && style != null){
                events = eventServices.getEventsByTypeAndStyle(type, style);

            } else if (type != null) {
                events = eventServices.getEventsByType(type);

            } else if(style != null){
                events = eventServices.getEventsByStyle(style);

            }
            else {
                events = eventServices.getAllEvents();
            }

            return ResponseEntity.ok(events);

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
    public ResponseEntity<?> createEvent(@Valid @RequestBody CreateEventDTO dto, @RequestHeader String authorization){
        Boolean token = tokenUtil.validateToken(authorization);

        if(!token){
            return ResponseEntity.status(401).body("Access denied");
        }

        try{
            return ResponseEntity.ok(eventServices.createEvent(dto, authorization));
        }catch (NotFoundException exception){
            return ResponseEntity.notFound().build();
        }catch(BadRequestException exception){
            return ResponseEntity.badRequest().body(exception.getMessage());
        }catch (RuntimeException exception){
            return ResponseEntity.internalServerError().body(exception.getMessage());
        }
    }

    @PatchMapping("/{id}/join")
    public ResponseEntity<?> JoinEvent(@PathVariable Long id, @RequestHeader String authorization){
        Boolean token = tokenUtil.validateToken(authorization);

        if(!token){
            return ResponseEntity.status(401).body("Access denied");
        }

        try{
            return ResponseEntity.ok(eventServices.joinEvent(authorization, id));
        }catch (NotFoundException exception){
            return ResponseEntity.notFound().build();
        }catch(BadRequestException exception){
            return ResponseEntity.badRequest().body(exception.getMessage());
        }catch (RuntimeException exception){
            return ResponseEntity.internalServerError().body(exception.getMessage());
        }
    }

    @PatchMapping("/{id}/leave")
    public ResponseEntity<?> leaveEvent(@PathVariable Long id, @RequestHeader String authorization){
        Boolean token = tokenUtil.validateToken(authorization);

        if(!token){
            return ResponseEntity.status(401).body("Access denied");
        }

        try{
            return ResponseEntity.ok(eventServices.leaveEvent(authorization, id));
        }catch (NotFoundException exception){
            return ResponseEntity.notFound().build();
        }catch (BadRequestException exception){
            return ResponseEntity.badRequest().body(exception.getMessage());
        }catch (RuntimeException exception){
            return ResponseEntity.internalServerError().body(exception.getMessage());
        }
    }

    @PatchMapping("/{eventId}/invite/user/{userId}")
    public ResponseEntity<?> inviteUser(@PathVariable Long eventId, @PathVariable Long userId, @RequestHeader String authorization){
        Boolean token = tokenUtil.validateToken(authorization);

        if(!token){
            return ResponseEntity.status(401).body("Access denied");
        }

        try{
            return ResponseEntity.ok(eventServices.inviteUser(authorization, eventId, userId));
        }catch (UnauthorizedException e){
            return ResponseEntity.status(403).body(e.getMessage());
        }catch (BadRequestException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (NotFoundException e){
            return ResponseEntity.notFound().build();
        }catch (RuntimeException e){
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @DeleteMapping("/{eventId}/kick/user/{userId}")
    public ResponseEntity<?> kickUser(@PathVariable Long eventId, @PathVariable Long userId, @RequestHeader String authorization ){
        Boolean token = tokenUtil.validateToken(authorization);

        if(!token){
            return ResponseEntity.status(401).body("Access denied");
        }

        try{
            return ResponseEntity.ok(eventServices.kickUser(authorization, eventId, userId));
        }catch (UnauthorizedException e){
            return ResponseEntity.status(403).body(e.getMessage());
        }catch (BadRequestException e){
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
            return ResponseEntity.status(403).body(e.getMessage());
        }catch (NotFoundException e){
            return ResponseEntity.notFound().build();
        }catch (RuntimeException e){
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }




    //Gestionar exceptions y docs



}

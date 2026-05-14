package com.afterApp.after.controller;

import com.afterApp.after.entity.Users;
import com.afterApp.after.exceptions.AlreadyExistsException;
import com.afterApp.after.exceptions.BadRequestException;
import com.afterApp.after.exceptions.FormatRequestException;
import com.afterApp.after.exceptions.NotFoundException;
import com.afterApp.after.service.UserServices;
import com.afterApp.after.utils.TokenUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {
    @Autowired
    private UserServices userServices;

    @Autowired
    TokenUtil tokenUtil;

    //Falta get de UserDetails

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id, @RequestHeader String authorization){
        Boolean token = tokenUtil.validateToken(authorization);

        if(!token){
            return ResponseEntity.status(401).body("Access denied");
        }

        try{
            return ResponseEntity.ok(userServices.getUserById(id));
        } catch (NotFoundException e){
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e){
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @Valid @RequestBody Users uDetails, @RequestHeader String authorization){
        Boolean token = tokenUtil.validateToken(authorization);

        if(!token){
            return ResponseEntity.status(401).body("Access denied");
        }

        try {
            return ResponseEntity.ok(userServices.updateUser(id, uDetails, authorization));
        }catch (NotFoundException e){
            return ResponseEntity.notFound().build();
        }catch (BadRequestException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }catch (RuntimeException e){
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/display-name")
    public ResponseEntity<?> updateDisplayName(@PathVariable Long id, @RequestHeader String authorization, @RequestBody Users uDetails){
        Boolean token = tokenUtil.validateToken(authorization);

        if(!token){
            return ResponseEntity.status(401).body("Access denied");
        }

        try{
            return ResponseEntity.ok(userServices.updateDisplayName(id, authorization, uDetails));
        }catch (NotFoundException e){
            return ResponseEntity.notFound().build();
        }catch (BadRequestException | FormatRequestException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (AlreadyExistsException e){
            return ResponseEntity.status(409).body(e.getMessage());
        } catch (RuntimeException e){
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}

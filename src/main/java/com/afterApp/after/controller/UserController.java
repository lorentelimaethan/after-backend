package com.afterApp.after.controller;

import com.afterApp.after.entity.User;
import com.afterApp.after.exceptions.AlreadyExistsException;
import com.afterApp.after.exceptions.FormatRequestException;
import com.afterApp.after.exceptions.NotFoundException;
import com.afterApp.after.service.UserServices;
import com.afterApp.after.utils.TokenUtil;
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
            return ResponseEntity.status(401).body("Acces denied");
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
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User uDetails, @RequestHeader String authorization){
        Boolean token = tokenUtil.validateToken(authorization);

        if(!token){
            return ResponseEntity.status(401).body("Acces denied");
        }

        try {
            User u = userServices.getUserById(id);

            u.setName(uDetails.getName());
            u.setLastname(uDetails.getLastname());
            u.setPhoneNumber(uDetails.getPhoneNumber());
            u.setEmail(uDetails.getEmail());
            u.setDisplayName(uDetails.getDisplayName());

            return ResponseEntity.ok(userServices.saveUser(u));
        }catch (NotFoundException e){
            return ResponseEntity.notFound().build();
        }catch (FormatRequestException | AlreadyExistsException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }catch (RuntimeException e){
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}

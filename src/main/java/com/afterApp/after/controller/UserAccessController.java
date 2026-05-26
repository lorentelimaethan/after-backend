package com.afterApp.after.controller;

import com.afterApp.after.dto.LoginDTO;
import com.afterApp.after.dto.RegisterDTO;
import com.afterApp.after.entity.UserAccess;
import com.afterApp.after.exceptions.BadRequestException;
import com.afterApp.after.service.UserAccessServices;
import com.afterApp.after.utils.TokenUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/token")
public class UserAccessController {
    @Autowired
    UserAccessServices userAccessServices;

    @Autowired
    private TokenUtil tokenUtil;

    @PostMapping("/auth/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterDTO dto){
        try{
            userAccessServices.registerUser(dto);
            return ResponseEntity.ok("Usuario creado correctamente");
        }catch (BadRequestException | DataIntegrityViolationException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e){
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> createToken(@RequestBody LoginDTO dto){
        if (userAccessServices.validateUser(dto)) {
            String token = tokenUtil.generateToken((dto.getUsername()));
            return ResponseEntity.ok(token);
        }else{
            return ResponseEntity.status(401).body("Access denied");
        }
    }


}

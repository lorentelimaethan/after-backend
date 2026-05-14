package com.afterApp.after.controller;

import com.afterApp.after.entity.UserAccess;
import com.afterApp.after.service.UserAccessServices;
import com.afterApp.after.utils.TokenUtil;
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

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody UserAccess userAccess){
        try{
            userAccessServices.registerUser(userAccess);
            return ResponseEntity.ok("Usuario creado correctamente");
        }catch (DataIntegrityViolationException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }catch (RuntimeException e){
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> createToken(@RequestBody UserAccess userAccess){
        if (userAccessServices.validateUser(userAccess)) {
            String token = tokenUtil.generateToken((userAccess.getUsername()));
            return ResponseEntity.ok(token);
        }else{
            return ResponseEntity.status(401).body("Access denied");
        }
    }


}

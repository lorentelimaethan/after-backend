package com.afterApp.after.service;

import com.afterApp.after.dto.LoginDTO;
import com.afterApp.after.dto.RegisterDTO;
import com.afterApp.after.entity.UserAccess;
import com.afterApp.after.entity.Users;
import com.afterApp.after.exceptions.BadRequestException;
import com.afterApp.after.repositories.UserAccessRepository;
import com.afterApp.after.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserAccessServices {
    @Autowired
    private UserAccessRepository userAccessRepository;

    @Autowired
    private UserRepository userRepository;

    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(16);

    public UserAccess registerUser(RegisterDTO dto){

        if(userAccessRepository.existsByUsername(dto.getUsername())){
            throw new BadRequestException("Username already exists");
        }

        UserAccess u = new UserAccess();

        u.setUsername(dto.getUsername());
        u.setPassword(encoder.encode((dto.getPassword())));

        Users user = new Users();
        user.setDisplayName(dto.getUsername());

        u.setUser(user);

        return userAccessRepository.save(u);
    }

    public boolean validateUser(LoginDTO dto){
        Optional<UserAccess> userAccess = userAccessRepository.findByUsername(dto.getUsername());

        if(userAccess.isPresent()){
            UserAccess access = userAccess.get();

            return encoder.matches(dto.getPassword(), access.getPassword());
        }
        return false;
    }
}

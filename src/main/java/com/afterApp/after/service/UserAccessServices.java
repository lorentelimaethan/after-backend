package com.afterApp.after.service;

import com.afterApp.after.entity.UserAccess;
import com.afterApp.after.entity.User;
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

    public UserAccess registerUser(UserAccess u){
        u.setPassword(encoder.encode((u.getPassword())));

        User user = new User();
        user.setDisplayName(u.getUsername());

        u.setUser(user);

        return userAccessRepository.save(u);
    }

    public boolean validateUser(UserAccess u){
        Optional<UserAccess> userAccess = userAccessRepository.findByUsername(u.getUsername());

        if(userAccess.isPresent()){
            UserAccess access = userAccess.get();

            return encoder.matches(u.getPassword(), access.getPassword());
        }
        return false;
    }
}

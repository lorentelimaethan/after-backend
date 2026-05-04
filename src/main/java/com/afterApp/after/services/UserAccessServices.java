package com.afterApp.after.services;

import com.afterApp.after.data.UserAccess;
import com.afterApp.after.data.User;
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

        UserAccess savedAccess = userAccessRepository.save(u);

        User user = new User();
        user.setDisplayName(u.getUsername());
        user.setUserAccess(savedAccess);

        userRepository.save(user);

        return savedAccess;
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

package com.afterApp.after.services;

import com.afterApp.after.data.UserAccess;
import com.afterApp.after.repositories.UserAccessRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserAccessServices {
    @Autowired
    private UserAccessRepository userAccessRepository;
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(16);

    public UserAccess saveUser(UserAccess u){
        u.setPassword(encoder.encode((u.getPassword())));
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

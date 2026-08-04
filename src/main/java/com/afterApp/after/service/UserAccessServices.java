package com.afterApp.after.service;

import com.afterApp.after.dto.LoginDTO;
import com.afterApp.after.entity.UserAccess;
import com.afterApp.after.entity.Users;
import com.afterApp.after.loader.UserAccessLoader;
import com.afterApp.after.mappers.UserAccessMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserAccessServices {
    @Autowired
    private UserAccessLoader userAccessLoader;

    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(16);

    public UserAccess registerUser(LoginDTO dto){

        UserAccess access = UserAccessMapper.fromDto(dto);
        access.setPassword(encoder.encode(dto.getPassword()));

        userAccessLoader.existByUsername(access);

        Users user = new Users();
        user.setDisplayName(dto.getUsername());

        access.setUser(user);

        return userAccessLoader.saveUserAccess(access);
    }

    public boolean validateUser(LoginDTO dto){
        UserAccess usrac = UserAccessMapper.fromDto(dto);

        Optional<UserAccess> userAccess = userAccessLoader.findByUsername(usrac);

        if(userAccess.isPresent()){
            UserAccess access = userAccess.get();

            return encoder.matches(usrac.getPassword(), access.getPassword());
        }
        return false;
    }
}

package com.afterApp.after.loader;

import com.afterApp.after.dto.LoginDTO;
import com.afterApp.after.entity.UserAccess;
import com.afterApp.after.exceptions.BadRequestException;
import com.afterApp.after.repositories.UserAccessRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserAccessLoader {
    @Autowired
    UserAccessRepository userAccessRepository;

    public void existByUsername(UserAccess access){
        if(userAccessRepository.existsByUsername(access.getUsername())){
            throw new BadRequestException("Username already exists");
        }
    }

    public UserAccess saveUserAccess(UserAccess u){
        return userAccessRepository.save(u);
    }

    public Optional<UserAccess> findByUsername(UserAccess access){
        return userAccessRepository.findByUsername(access.getUsername());
    }
}

package com.afterApp.after.loader;

import com.afterApp.after.dto.UpdateDisplayNameDTO;
import com.afterApp.after.entity.UserAccess;
import com.afterApp.after.entity.Users;
import com.afterApp.after.exceptions.AlreadyExistsException;
import com.afterApp.after.exceptions.NotFoundException;
import com.afterApp.after.repositories.UserAccessRepository;
import com.afterApp.after.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserLoader {
    @Autowired
    UserRepository userRepository;
    @Autowired
    UserAccessRepository userAccessRepository;

    public UserAccess findByUsername(String username){
        return userAccessRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not Found"));
    }

    public Users findById(Long id){
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
    }

    public Users saveUser(Users u){
        return userRepository.save(u);
    }

    public void existByDisplayName(UpdateDisplayNameDTO uDetails){
        if(userRepository.existsByDisplayName(uDetails.getDisplayName())){
            throw new AlreadyExistsException("Already Existing username");
        }
    }

    public void deleteUser(Users user){
        UserAccess userAccess = userAccessRepository.findByUserId(user.getId())
                .orElseThrow(() -> new NotFoundException("User access not found"));
        userAccessRepository.delete(userAccess);
    }
}

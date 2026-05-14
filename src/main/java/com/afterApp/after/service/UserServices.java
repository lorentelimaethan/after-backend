package com.afterApp.after.service;

import com.afterApp.after.entity.Users;
import com.afterApp.after.entity.UserAccess;
import com.afterApp.after.exceptions.AlreadyExistsException;
import com.afterApp.after.exceptions.BadRequestException;
import com.afterApp.after.exceptions.FormatRequestException;
import com.afterApp.after.exceptions.NotFoundException;
import com.afterApp.after.repositories.UserAccessRepository;
import com.afterApp.after.repositories.UserRepository;
import com.afterApp.after.utils.TokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServices {
    @Autowired
    UserRepository userRepository;

    @Autowired
    private TokenUtil tokenUtil;

    @Autowired
    private UserAccessRepository userAccessRepository;


    private Users extractUser(String authorization){
        String jwt = authorization.replace("Bearer ", "");
        String username = tokenUtil.extractUsername(jwt);

        UserAccess userAccess = userAccessRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not Found"));

        return userAccess.getUser();
    }

    public List<Users> getAllUsers() { return userRepository.findAll(); }

    public Users getUserById(Long id) throws RuntimeException{
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not Found"));
    }

    public Users updateUser(Long id, Users uDetails, String authorization){
        Users requester = extractUser(authorization);

        if (!requester.getId().equals(id)){
            throw new BadRequestException("You can only update your own profile");
        }

        Users u = getUserById(id);

        if(uDetails.getName() != null) {u.setName(uDetails.getName());}
        if(uDetails.getLastname() != null) {u.setLastname(uDetails.getLastname());}
        if(uDetails.getPhoneNumber() != null) {u.setPhoneNumber(uDetails.getPhoneNumber());}
        if(uDetails.getEmail() != null) {u.setEmail(uDetails.getEmail());}

        return userRepository.save(u);
    }

    public Users updateDisplayName(Long id, String authorization, Users uDetails){
        Users requester = extractUser(authorization);

        if (!requester.getId().equals(id)){
            throw new BadRequestException("You can only update your own profile");
        }

        Users u = getUserById(id);

        String newDisplayName = uDetails.getDisplayName();

        if(newDisplayName == null || newDisplayName.isBlank()){
            throw new BadRequestException("DisplayName cannot be null or empty");
        }

        if(!uDetails.getDisplayName().matches("^[a-zA-Z0-9](?:[a-zA-Z0-9._]{1,18}[a-zA-Z0-9])?$")){
            throw new FormatRequestException("Incorrect username");
        }

        if(userRepository.existsByDisplayName(uDetails.getDisplayName())){
            throw new AlreadyExistsException("Already Existing username");
        }

        u.setDisplayName(newDisplayName);

        return userRepository.save(u);
    }


}

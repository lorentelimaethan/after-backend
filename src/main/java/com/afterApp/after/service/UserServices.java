package com.afterApp.after.service;

import com.afterApp.after.entity.User;
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


    private User extractUser(String authorization){
        String jwt = authorization.replace("Bearer ", "");
        String username = tokenUtil.extractUsername(jwt);

        UserAccess userAccess = userAccessRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not Found"));

        return userAccess.getUser();
    }

    public List<User> getAllUsers() { return userRepository.findAll(); }

    public User getUserById(Long id) throws RuntimeException{
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not Found"));
    }

    public User saveUser(User u) throws RuntimeException{
        if(u.getPhoneNumber() == null || u.getEmail() == null){
            throw new BadRequestException("Phonenumber and email must be valid");
        }

        if(!u.getPhoneNumber().matches("^\\+?[0-9]{7,15}$")){
            throw new FormatRequestException("Incorrect Number");
        } else if (!u.getEmail().matches(".+@.+\\..+")){
            throw new FormatRequestException("Incorrect Email");
        } else if(!u.getDisplayName().matches("^[a-zA-Z0-9](?:[a-zA-Z0-9._]{1,18}[a-zA-Z0-9])?$")){
            throw new FormatRequestException("Incorrect username");
        } else if(userRepository.existsByDisplayName(u.getDisplayName())){
            throw new AlreadyExistsException("Already Existing username");
        } else {
            return userRepository.save(u);
        }
    }

    public User updateUser(Long id, User uDetails, String authorization){
        User requester = extractUser(authorization);

        User u = getUserById(id);

        if (!requester.getId().equals(u.getId())){
            throw new BadRequestException("You can only update your own profile");
        }

        u.setName(uDetails.getName());
        u.setLastname(uDetails.getLastname());
        u.setPhoneNumber(uDetails.getPhoneNumber());
        u.setEmail(uDetails.getEmail());
        u.setDisplayName(uDetails.getDisplayName());

        return saveUser(u);
    }


}

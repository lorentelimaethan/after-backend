package com.afterApp.after.services;

import com.afterApp.after.data.User;
import com.afterApp.after.exceptions.AlreadyExistsException;
import com.afterApp.after.exceptions.FormatRequestException;
import com.afterApp.after.exceptions.NotFoundException;
import com.afterApp.after.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServices {
    @Autowired
    UserRepository userRepository;

    public List<User> getAllUsers() { return userRepository.findAll(); }

    public User getUserById(Long id) throws RuntimeException{
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not Found"));
    }

    public User saveUser(User u) throws RuntimeException{
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

}

package com.afterApp.after.service;

import com.afterApp.after.dto.UpdateDisplayNameDTO;
import com.afterApp.after.dto.UpdateUserDTO;
import com.afterApp.after.dto.UserResponseDTO;
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

    public UserResponseDTO toDto(Users u){
        UserResponseDTO dto = new UserResponseDTO();

        dto.setId(u.getId());
        dto.setName(u.getName());
        dto.setLastname(u.getLastname());
        dto.setEmail(u.getEmail());
        dto.setPhoneNumber(u.getPhoneNumber());
        dto.setDisplayName(u.getDisplayName());

        return dto;
    }

    public UserResponseDTO getUserById(Long id) throws RuntimeException{
        Users u = userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not Found"));

        return toDto(u);
    }

    public Users getUserEntityById(Long id) throws RuntimeException{
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
    }

    public UserResponseDTO updateUser(Long id, UpdateUserDTO uDtoDetails, String authorization){
        Users requester = extractUser(authorization);

        if (!requester.getId().equals(id)){
            throw new BadRequestException("You can only update your own profile");
        }

        Users u = getUserEntityById(id);

        if(uDtoDetails.getName() != null) {u.setName(uDtoDetails.getName());}
        if(uDtoDetails.getLastname() != null) {u.setLastname(uDtoDetails.getLastname());}
        if(uDtoDetails.getPhoneNumber() != null) {u.setPhoneNumber(uDtoDetails.getPhoneNumber());}
        if(uDtoDetails.getEmail() != null) {u.setEmail(uDtoDetails.getEmail());}

        return toDto(userRepository.save(u));
    }

    public UserResponseDTO updateDisplayName(Long id, String authorization, UpdateDisplayNameDTO uDetails){
        Users requester = extractUser(authorization);

        if (!requester.getId().equals(id)){
            throw new BadRequestException("You can only update your own profile");
        }

        Users u = getUserEntityById(id);

        String newDisplayName = uDetails.getDisplayName();

        if(userRepository.existsByDisplayName(uDetails.getDisplayName())){
            throw new AlreadyExistsException("Already Existing username");
        }

        u.setDisplayName(newDisplayName);

        return toDto(userRepository.save(u));
    }


}

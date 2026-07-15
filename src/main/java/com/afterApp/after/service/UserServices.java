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
import com.afterApp.after.loader.UserLoader;
import com.afterApp.after.repositories.UserAccessRepository;
import com.afterApp.after.repositories.UserRepository;
import com.afterApp.after.utils.TokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.afterApp.after.mappers.UserMapper.toDto;
import static com.afterApp.after.mappers.UserMapper.updateUserData;

@Service
public class UserServices {
    @Autowired
    UserRepository userRepository;
    @Autowired
    private TokenUtil tokenUtil;
    @Autowired
    private UserAccessRepository userAccessRepository;
    @Autowired
    UserLoader userLoader;


    private Users extractUser(String authorization){
        String jwt = authorization.replace("Bearer ", "");
        String username = tokenUtil.extractUsername(jwt);

        UserAccess userAccess = userLoader.findByUsername(username);

        return userAccess.getUser();
    }

    public UserResponseDTO getUserById(Long id) throws RuntimeException{
        Users u = userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not Found"));

        return toDto(u);
    }

    public Users getUserEntityById(Long id) throws RuntimeException{
        return userLoader.findById(id);
    }

    public UserResponseDTO updateUser(Long id, UpdateUserDTO uDtoDetails, String authorization){
        Users requester = extractUser(authorization);

        if (!requester.getId().equals(id)){
            throw new BadRequestException("You can only update your own profile");
        }

        Users u = getUserEntityById(id);

        updateUserData(u, uDtoDetails);

        return toDto(userLoader.saveUser(u)); // mapper + unitTest
    }

    public UserResponseDTO updateDisplayName(Long id, String authorization, UpdateDisplayNameDTO uDetails){
        Users requester = extractUser(authorization);

        if (!requester.getId().equals(id)){
            throw new BadRequestException("You can only update your own profile");
        }

        Users u = getUserEntityById(id);

        String newDisplayName = uDetails.getDisplayName();

        userLoader.existByDisplayName(uDetails);

        u.setDisplayName(newDisplayName);

        return toDto(userLoader.saveUser(u));
    }


}

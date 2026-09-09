package com.afterApp.after.service;

import com.afterApp.after.dto.UpdateDisplayNameDTO;
import com.afterApp.after.dto.UpdateRoleDTO;
import com.afterApp.after.dto.UpdateUserDTO;
import com.afterApp.after.dto.UserResponseDTO;
import com.afterApp.after.entity.UserRole;
import com.afterApp.after.entity.Users;
import com.afterApp.after.entity.UserAccess;
import com.afterApp.after.enums.Resources;
import com.afterApp.after.exceptions.*;
import com.afterApp.after.loader.UserLoader;
import com.afterApp.after.loader.UserRoleLoader;
import com.afterApp.after.repositories.UserAccessRepository;
import com.afterApp.after.repositories.UserRepository;
import com.afterApp.after.utils.TokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.afterApp.after.mappers.UserMapper.toDto;
import static com.afterApp.after.mappers.UserMapper.updateUserData;

@Service
public class UserServices {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TokenUtil tokenUtil;
    @Autowired
    private UserAccessRepository userAccessRepository;
    @Autowired
    private UserLoader userLoader;
    @Autowired
    private UserRoleLoader userRoleLoader;


    private Users extractUser(String authorization){
        String jwt = authorization.replace("Bearer ", "");
        String username = tokenUtil.extractUsername(jwt);

        UserAccess userAccess = userLoader.findByUsername(username);

        return userAccess.getUser();
    }

    @Cacheable(value = "users", key = "#id")
    public UserResponseDTO getUserById(Long id) throws RuntimeException{
        Users u = getUserEntityById(id);

        return toDto(u);
    }

    public Users getUserEntityById(Long id) throws RuntimeException{
        return userLoader.findById(id);
    }

    @CacheEvict(value = "users", key = "#id")
    public UserResponseDTO updateUser(Long id, UpdateUserDTO uDtoDetails, String authorization){
        Users requester = extractUser(authorization);

        if (!requester.getId().equals(id)){
            throw new BadRequestException("You can only update your own profile");
        }

        Users u = getUserEntityById(id);

        updateUserData(u, uDtoDetails);

        return toDto(userLoader.saveUser(u));
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

    public UserResponseDTO updateUserRole(Long userId, UpdateRoleDTO dto, String authorization){
        Users requester = extractUser(authorization);

        if (requester.getUserRole() == null ||
                !requester.getUserRole().getResources().contains(Resources.UPDATE_ROLE)) {
            throw new UnauthorizedException("Only users with UPDATE_ROLE can update roles"); //PREGUNTAR A ALEX
        }

        Users targetUser = userLoader.findById(userId);

        UserRole newRole = userRoleLoader.findByRoleName(dto.getRoleName());

        targetUser.setUserRole(newRole);

        return toDto(userLoader.saveUser(targetUser));
    }

    @CacheEvict(value = "users", key = "#id")
    public void deleteUser(Long id, String authorization){
        Users requester = extractUser(authorization);

        Users target = userLoader.findById(id);

        if(!requester.getId().equals(id)){
            throw new BadRequestException("You can only delete your own profile");
        }

        userLoader.deleteUser(target);
    }

    //Hacer un extract role, meter en el .parser(username), crear un external id para el role id=free, id=admine etc.. añadir el id para extraerlo.
}

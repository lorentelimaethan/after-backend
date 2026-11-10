package com.afterApp.after.service;

import com.afterApp.after.dto.*;
import com.afterApp.after.entity.Events;
import com.afterApp.after.entity.UserRole;
import com.afterApp.after.entity.Users;
import com.afterApp.after.entity.UserAccess;
import com.afterApp.after.enums.Resources;
import com.afterApp.after.exceptions.*;
import com.afterApp.after.loader.EventLoader;
import com.afterApp.after.loader.UserLoader;
import com.afterApp.after.loader.UserRoleLoader;
import com.afterApp.after.repositories.UserAccessRepository;
import com.afterApp.after.repositories.UserRepository;
import com.afterApp.after.utils.TokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static com.afterApp.after.mappers.UserMapper.*;

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
    @Autowired
    private EventLoader eventLoader;
    @Autowired
    private CacheManager cacheManager;

    private static final Logger log = LoggerFactory.getLogger(UserServices.class);

    private Users extractUser(String authorization){
        String jwt = authorization.replace("Bearer ", "");
        String username = tokenUtil.extractUsername(jwt);

        UserAccess userAccess = userLoader.findByUsername(username);

        return userAccess.getUser();
    }


    public UserPublicResponseDTO getUserById(Long id, String authorization) throws RuntimeException{
        log.info("getting user with Id = {}", id);

        Users requester = extractUser(authorization);

        Users u = getUserEntityById(id);

        if(requester.getId().equals(id)){
            return toPrivateDto(u);
        }


        return toPublicDto(u);
    }

    public Users getUserEntityById(Long id) throws RuntimeException{
        return userLoader.findById(id);
    }

    @CacheEvict(value = "users", key = "#id")
    public UserPrivateResponseDTO updateUser(Long id, UpdateUserDTO uDtoDetails, String authorization){
        Users requester = extractUser(authorization);

        if (!requester.getId().equals(id)){
            throw new BadRequestException("You can only update your own profile");
        }

        Users u = getUserEntityById(id);

        updateUserData(u, uDtoDetails);

        return toPrivateDto(userLoader.saveUser(u));
    }

    @CacheEvict(value = "users", key = "#id")
    public UserPrivateResponseDTO updateDisplayName(Long id, String authorization, UpdateDisplayNameDTO uDetails){
        Users requester = extractUser(authorization);

        if (!requester.getId().equals(id)){
            throw new BadRequestException("You can only update your own profile");
        }

        Users u = getUserEntityById(id);

        String newDisplayName = uDetails.getDisplayName();

        userLoader.existByDisplayName(uDetails);

        u.setDisplayName(newDisplayName);

        return toPrivateDto(userLoader.saveUser(u));
    }

    @CacheEvict(value = "users", key = "#userId")
    public UserPrivateResponseDTO updateUserRole(Long userId, UpdateRoleDTO dto, String authorization){
        Users requester = extractUser(authorization);

        if (requester.getUserRole() == null ||
                !requester.getUserRole().getResources().contains(Resources.UPDATE_ROLE)) {
            throw new UnauthorizedException("Only users with UPDATE_ROLE can update roles");
        }

        Users targetUser = userLoader.findById(userId);

        UserRole newRole = userRoleLoader.findByRoleName(dto.getRoleName());

        targetUser.setUserRole(newRole);

        return toPrivateDto(userLoader.saveUser(targetUser));
    }


    @Transactional
    public void deleteUser(Long id, String authorization){
        Users requester = extractUser(authorization);

        if(!requester.getId().equals(id)){
            throw new UnauthorizedException("You can only delete your own profile");
        }

        Users target = userLoader.findById(id);

        List<Events> attendedEvents = eventLoader.findAttendedByUserId(target.getId());
        List<Events> hostedEvents = eventLoader.findHostedByUserId(target.getId());

        for(Events event : attendedEvents){
            event.getUsers().removeIf(attendee ->
                    target.getId().equals(attendee.getId()));
        }

        eventLoader.saveAllAndFlush(attendedEvents);
        eventLoader.deleteAllAndFlush(hostedEvents);
        userLoader.deleteUser(target);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit(){
                Cache usersCache = cacheManager.getCache("users");

                if (usersCache != null){
                    usersCache.evict(id);
                }

                for (String name : List.of(
                        "events", "eventsByType", "eventsByStyle", "eventsByTypeStyle")){
                    Cache cache = cacheManager.getCache(name);
                    if(cache != null){
                        cache.clear();
                    }
                }
            }
        });
    }

}

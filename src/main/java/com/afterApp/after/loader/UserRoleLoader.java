package com.afterApp.after.loader;

import com.afterApp.after.entity.UserRole;
import com.afterApp.after.exceptions.NotFoundException;
import com.afterApp.after.repositories.UserRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserRoleLoader {
    @Autowired
    private UserRoleRepository userRoleRepository;

    public UserRole findByRoleName(String roleName){
        return userRoleRepository.findByRoleName(roleName).orElseThrow(() -> new NotFoundException("Role not found"));
    }

}

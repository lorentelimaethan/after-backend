package com.afterApp.after.loader;

import com.afterApp.after.entity.UserRole;
import com.afterApp.after.enums.Resources;
import com.afterApp.after.repositories.UserRoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserRoleInitializer implements CommandLineRunner {
    private final UserRoleRepository userRoleRepository;

    public UserRoleInitializer(UserRoleRepository userRoleRepository) {
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    public void run(String... args) {
        createRoleIfNotExists("FREE", List.of(
                Resources.VIEW_EVENT,
                Resources.CREATE_EVENT,
                Resources.ACCESS_EVENT,
                Resources.UPDATE_USER,
                Resources.VIEW_USER
        ));

        createRoleIfNotExists("PREMIUM", List.of(
                Resources.VIEW_EVENT,
                Resources.CREATE_EVENT,
                Resources.ACCESS_EVENT,
                Resources.INVITE_USER
        ));

        createRoleIfNotExists("ADMIN", List.of(
                Resources.VIEW_EVENT,
                Resources.CREATE_EVENT,
                Resources.ACCESS_EVENT,
                Resources.INVITE_USER,
                Resources.KICK_USER,
                Resources.DELETE_EVENT,
                Resources.VIEW_USER,
                Resources.UPDATE_USER,
                Resources.UPDATE_ROLE
        ));
    }

    private void createRoleIfNotExists(String roleName, List<Resources> resources) {
        if (userRoleRepository.findByRoleName(roleName).isPresent()) {
            return;
        }

        UserRole role = new UserRole();
        role.setRoleName(roleName);
        role.setResources(resources);

        userRoleRepository.save(role);
    }

    //crear admin;
    //añadir premium = free + cosas etc..
}

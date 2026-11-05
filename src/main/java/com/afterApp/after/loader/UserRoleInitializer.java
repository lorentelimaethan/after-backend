package com.afterApp.after.loader;

import com.afterApp.after.entity.UserRole;
import com.afterApp.after.entity.Users;
import com.afterApp.after.enums.Resources;
import com.afterApp.after.repositories.UserRepository;
import com.afterApp.after.repositories.UserRoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Component
@Order(1)
public class UserRoleInitializer implements CommandLineRunner {
    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;

    public UserRoleInitializer(UserRoleRepository userRoleRepository,
                               UserRepository userRepository) {
        this.userRoleRepository = userRoleRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        List<Resources> freeResources = List.of(
                Resources.VIEW_EVENT,
                Resources.CREATE_EVENT,
                Resources.ACCESS_EVENT,
                Resources.UPDATE_USER,
                Resources.VIEW_USER
        );

        List<Resources> premiumResources = mergeResources(
                freeResources,
                Resources.INVITE_USER
        );

        List<Resources> adminResources = mergeResources(
                premiumResources,
                Resources.KICK_USER,
                Resources.DELETE_EVENT,
                Resources.UPDATE_ROLE,
                Resources.VIEW_METRICS
        );

        UserRole freeRole = createOrUpdateRole("FREE", freeResources);
        createOrUpdateRole("PREMIUM", premiumResources);
        createOrUpdateRole("ADMIN", adminResources);

        assignFreeRoleToUsersWithoutRole(freeRole);
    }

    private UserRole createOrUpdateRole(String roleName, List<Resources> resources) {
        UserRole role = userRoleRepository.findByRoleName(roleName)
                .orElseGet(UserRole::new);

        role.setRoleName(roleName);
        role.setResources(resources);

        return userRoleRepository.save(role);
    }

    private void assignFreeRoleToUsersWithoutRole(UserRole freeRole){
        List<Users> withOutRole = userRepository.findByUserRoleIsNull();

        withOutRole.forEach(users -> users.setUserRole(freeRole));

        userRepository.saveAll(withOutRole);
    }

    private List<Resources> mergeResources(List<Resources> baseResources, Resources... extraResources) {
        LinkedHashSet<Resources> mergedResources = new LinkedHashSet<>(baseResources);
        mergedResources.addAll(List.of(extraResources));

        return new ArrayList<>(mergedResources);
    }
}

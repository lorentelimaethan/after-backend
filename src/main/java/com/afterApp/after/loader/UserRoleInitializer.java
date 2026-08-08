package com.afterApp.after.loader;

import com.afterApp.after.entity.UserAccess;
import com.afterApp.after.entity.UserRole;
import com.afterApp.after.entity.Users;
import com.afterApp.after.enums.Resources;
import com.afterApp.after.repositories.UserAccessRepository;
import com.afterApp.after.repositories.UserRoleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Component
public class UserRoleInitializer implements CommandLineRunner {
    private final UserRoleRepository userRoleRepository;
    private final UserAccessRepository userAccessRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(16);

    @Value("${after.admin.username:admin}")
    private String adminUsername;

    @Value("${after.admin.password:admin123}")
    private String adminPassword;

    public UserRoleInitializer(UserRoleRepository userRoleRepository, UserAccessRepository userAccessRepository) {
        this.userRoleRepository = userRoleRepository;
        this.userAccessRepository = userAccessRepository;
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
                Resources.UPDATE_ROLE
        );

        createOrUpdateRole("FREE", freeResources);
        createOrUpdateRole("PREMIUM", premiumResources);
        UserRole adminRole = createOrUpdateRole("ADMIN", adminResources);

        createAdminIfNotExists(adminRole);
    }

    private UserRole createOrUpdateRole(String roleName, List<Resources> resources) {
        UserRole role = userRoleRepository.findByRoleName(roleName)
                .orElseGet(UserRole::new);

        role.setRoleName(roleName);
        role.setResources(resources);

        return userRoleRepository.save(role);
    }

    private List<Resources> mergeResources(List<Resources> baseResources, Resources... extraResources) {
        LinkedHashSet<Resources> mergedResources = new LinkedHashSet<>(baseResources);
        mergedResources.addAll(List.of(extraResources));

        return new ArrayList<>(mergedResources);
    }

    private void createAdminIfNotExists(UserRole adminRole) {
        if (userAccessRepository.existsByUsername(adminUsername)) {
            return;
        }

        Users adminUser = new Users();
        adminUser.setDisplayName(adminUsername);
        adminUser.setUserRole(adminRole);

        UserAccess adminAccess = new UserAccess();
        adminAccess.setUsername(adminUsername);
        adminAccess.setPassword(encoder.encode(adminPassword));
        adminAccess.setUser(adminUser);

        userAccessRepository.save(adminAccess);
    }
}

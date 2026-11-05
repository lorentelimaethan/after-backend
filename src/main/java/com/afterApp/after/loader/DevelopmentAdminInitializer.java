package com.afterApp.after.loader;
import com.afterApp.after.entity.UserAccess;
import com.afterApp.after.entity.UserRole;
import com.afterApp.after.entity.Users;
import com.afterApp.after.repositories.UserAccessRepository;
import com.afterApp.after.repositories.UserRoleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev", "test"})
@Order(2)
public class DevelopmentAdminInitializer implements CommandLineRunner {
    private final UserRoleRepository userRoleRepository;
    private final UserAccessRepository userAccessRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(16);

    public DevelopmentAdminInitializer(
            UserRoleRepository userRoleRepository,
            UserAccessRepository userAccessRepository){
        this.userRoleRepository = userRoleRepository;
        this.userAccessRepository = userAccessRepository;
    }

    @Value("${after.admin.username}")
    private String adminUsername;

    @Value("${after.admin.password}")
    private String adminPassword;

    @Value("${after.admin.name}")
    private String adminName;

    @Value("${after.admin.lastname}")
    private String adminLastname;

    @Value("${after.admin.email}")
    private String adminEmail;

    @Value("${after.admin.phone-number}")
    private String adminPhoneNumber;

    @Override
    public void run(String... args){
       UserRole adminRole = userRoleRepository.findByRoleName("ADMIN").orElseThrow(() -> new IllegalStateException(
                "The ADMIN role must exist before creating the development admin"
        ));

        createAdminIfNotExists(adminRole);
    }

    private void createAdminIfNotExists(UserRole adminRole) {
        if (userAccessRepository.existsByUsername(adminUsername)) {
            return;
        }

        Users adminUser = new Users();
        adminUser.setName(adminName);
        adminUser.setLastname(adminLastname);
        adminUser.setEmail(adminEmail);
        adminUser.setPhoneNumber(adminPhoneNumber);
        adminUser.setDisplayName(adminUsername);
        adminUser.setUserRole(adminRole);

        UserAccess adminAccess = new UserAccess();
        adminAccess.setUsername(adminUsername);
        adminAccess.setPassword(encoder.encode(adminPassword));
        adminAccess.setUser(adminUser);

        userAccessRepository.save(adminAccess);
    }
}

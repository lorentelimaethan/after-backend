package com.afterApp.after.repositories;

import com.afterApp.after.entity.UserAccess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccessRepository extends JpaRepository<UserAccess, Long> {
    Optional<UserAccess> findByUsername(String username);

    Boolean existsByUsername(String username);
}

package com.afterApp.after.repositories;

import com.afterApp.after.data.UserAccess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccessRepository extends JpaRepository<UserAccess, Long> {
    Optional<UserAccess> findByUsername(String username);
}

package com.afterApp.after.repositories;

import com.afterApp.after.data.Users;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<Users, Long> {
    boolean existsByDisplayName(String displayName);
}

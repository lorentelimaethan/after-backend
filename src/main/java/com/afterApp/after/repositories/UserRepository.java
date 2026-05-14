package com.afterApp.after.repositories;

import com.afterApp.after.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByDisplayName(String displayName);
}

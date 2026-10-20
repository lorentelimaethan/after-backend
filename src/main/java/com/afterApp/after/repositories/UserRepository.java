package com.afterApp.after.repositories;

import com.afterApp.after.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<Users, Long> {
    boolean existsByDisplayName(String displayName);
    List<Users> findByUserRoleIsNull();
}

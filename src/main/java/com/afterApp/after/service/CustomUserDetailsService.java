package com.afterApp.after.service;

import com.afterApp.after.entity.UserAccess;
import com.afterApp.after.repositories.UserAccessRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserAccessRepository userAccessRepository;

    public CustomUserDetailsService(UserAccessRepository userAccessRepository){
        this.userAccessRepository = userAccessRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException{
        UserAccess userAccess = userAccessRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException(username));

        System.out.println("UserAccess authorization " + userAccess.getUser().getUserRole());
        List<GrantedAuthority> authorities = userAccess.getUser()
                .getUserRole()
                .getResources()
                .stream()
                .map(resource -> (GrantedAuthority) new SimpleGrantedAuthority(resource.name()))
                .toList();


        return org.springframework.security.core.userdetails.User
                .withUsername(userAccess.getUsername())
                .password(userAccess.getPassword())
                .authorities(authorities)
                .build();
    }
}

package com.afterApp.after.utils;

import com.afterApp.after.entity.UserAccess;
import com.afterApp.after.entity.Users;
import com.afterApp.after.loader.UserLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
public class UserUtil {
    private final UserLoader userLoader;
    private final TokenUtil tokenUtil;

    public UserUtil(UserLoader userLoader, TokenUtil tokenUtil){
        this.userLoader = userLoader;
        this.tokenUtil = tokenUtil;
    }

    public Users extractUser(String authorization){
        String jwt = authorization.replace("Bearer ", "");
        String username = tokenUtil.extractUsername(jwt);

        UserAccess userAccess = userLoader.findByUsername(username);

        return userAccess.getUser();
    }
}

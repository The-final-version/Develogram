package com.goorm.clonestagram.util;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;

public class TestSecurityUtil {
    public static Authentication mockUserDetails(Long userId) {
        CustomUserDetails userDetails = new CustomUserDetails(
            UserEntity.builder()
                .id(userId)
                .name("mockuser")
                .email("mock@mock.com")
                .password("pass")
                .build()
        );
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }
}

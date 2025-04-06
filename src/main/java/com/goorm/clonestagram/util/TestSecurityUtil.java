package com.goorm.clonestagram.util;

import com.goorm.clonestagram.user.domain.Users;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

public class TestSecurityUtil {
    public static Authentication mockUserDetails(Long userId) {
        CustomUserDetails userDetails = new CustomUserDetails(
                Users.builder()
                        .id(userId)
                        .username("mockuser")
                        .email("mock@mock.com")
                        .password("pass")
                        .build()
        );
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }
}

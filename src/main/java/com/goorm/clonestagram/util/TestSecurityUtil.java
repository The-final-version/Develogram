package com.goorm.clonestagram.util;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.vo.UserEmail;
import com.goorm.clonestagram.user.domain.vo.UserName;
import com.goorm.clonestagram.user.domain.vo.UserPassword;

public class TestSecurityUtil {
    public static Authentication mockUserDetails(Long userId) {
        CustomUserDetails userDetails = new CustomUserDetails(
                User.builder()
                        .id(userId)
                        .name(new UserName("mockuser"))
                        .email(new UserEmail("mock@mock.com"))
                        .password(new UserPassword("pass"))
                        .build()
        );
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }
}

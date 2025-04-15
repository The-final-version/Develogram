package com.goorm.clonestagram.util;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class CustomUserDetails implements UserDetails {
    @NotBlank(message = "사용자 정보가 없습니다.")
    private final UserEntity user;

    public CustomUserDetails(UserEntity user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // 계정 만료 여부
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // 계정 잠금 여부
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // 자격 증명(비밀번호) 만료 여부
    }

    @Override
    public boolean isEnabled() {
        return true; // 계정 활성화 여부
    }

    public Long getId() {
        return user.getId(); // 실제 사용자 ID 반환
    }

}


package com.goorm.clonestagram.user.application.service.auth;

import com.goorm.clonestagram.user.application.adapter.UserAdapter;
import com.goorm.clonestagram.user.application.dto.auth.JoinDto;
import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.service.UserInternalQueryService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;

@Service
@Validated
@RequiredArgsConstructor
public class UserJoinService {

    private final UserInternalQueryService userInternalQueryService;

    public void joinProcess(@Valid JoinDto joinDto) {
        // 1) 이메일 중복 체크
        checkIfEmailAlreadyExists(joinDto.getEmail());

        // 2) 비밀번호 불일치 검사(도메인 로직)
        validatePassword(joinDto.getPassword(), joinDto.getConfirmPassword());

        // 3) 새 유저 엔티티 생성
        User newUser = UserAdapter.fromUserProfileDto(joinDto);

        // 4) DB 저장
        userInternalQueryService.saveUser(newUser);
    }

    private void checkIfEmailAlreadyExists(String email) {
        if (userInternalQueryService.existsByEmail(email)) {
            throw new IllegalStateException("이미 사용 중인 이메일입니다: " + email);
        }
    }

    private void validatePassword(String password, String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            throw new IllegalStateException("비밀번호가 일치하지 않습니다.");
        }
    }
}

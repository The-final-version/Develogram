package com.goorm.clonestagram.user.application.service.auth;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.goorm.clonestagram.common.exception.BusinessException;
import com.goorm.clonestagram.common.exception.ErrorCode;
import com.goorm.clonestagram.user.application.adapter.UserAdapter;
import com.goorm.clonestagram.user.application.dto.auth.JoinDto;
import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.service.UserInternalQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class UserJoinService {

    private final UserInternalQueryService userInternalQueryService;

    @Transactional
    public void joinProcess(JoinDto joinDto) {
        // 1) 이메일 중복 체크
        checkIfEmailAlreadyExists(joinDto.getEmail());

        // 2) 비밀번호 불일치 검사(도메인 로직)
        validatePassword(joinDto.getPassword(), joinDto.getConfirmPassword());

        try {
            // 3) 새 유저 엔티티 생성
            User newUser = UserAdapter.fromUserProfileDto(joinDto);

            // 4) DB 저장
            userInternalQueryService.saveUser(newUser);
            log.info("[JOIN] 신규 회원 가입: {}", newUser.getEmail());
        } catch (DataAccessException e) {
            log.error("[DB] 회원가입 저장 오류", e);
            throw new BusinessException(ErrorCode.USER_DATABASE_ERROR);
        }
    }

    private void checkIfEmailAlreadyExists(String email) {
        if (userInternalQueryService.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.USER_DUPLICATE_EMAIL);
        }
    }

    private void validatePassword(String password, String confirmPassword) {
        if(!password.equals(confirmPassword)) {
            throw new BusinessException(ErrorCode.USER_PASSWORD_MISMATCH);
        }
    }
}

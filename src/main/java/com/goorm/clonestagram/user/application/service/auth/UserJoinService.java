package com.goorm.clonestagram.user.application.service.auth;

import org.springframework.stereotype.Service;

import com.goorm.clonestagram.user.application.adapter.UsersAdapter;
import com.goorm.clonestagram.user.application.dto.auth.JoinDto;
import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.service.UserInternalQueryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserJoinService {
    private final UserInternalQueryService userInternalQueryService;

    /**
     * (public) 사용자 가입 처리
     * @param joinDto 가입 정보 DTO
     * @throws IllegalStateException 이메일 중복 또는 비밀번호 불일치 시 예외 발생
     */
    public void joinProcess(JoinDto joinDto) {
        // 1) 이메일 중복 체크
        checkIfEmailAlreadyExists(joinDto.getEmail());

        // 2) 비밀번호 검증
        validatePassword(joinDto.getPassword(), joinDto.getConfirmPassword());

        // 2) 새 유저 엔티티 생성
        User newUser = UsersAdapter.fromUserProfileDto(joinDto);

        // 3) DB 저장
        userInternalQueryService.saveUser(newUser);
    }

	/* =============================
       ( Private Methods )
   	============================= */

    /**
     * (private) 이메일 중복 검사
     * @param email 중복 검사할 이메일
     * @throws IllegalStateException 이미 사용 중인 이메일일 경우 예외 발생
     */
    private void checkIfEmailAlreadyExists(String email) {
        boolean exists = userInternalQueryService.existsByEmail(email);
        if (exists) {
            throw new IllegalStateException("이미 사용 중인 이메일입니다: " + email);
        }
    }

    /**
     * (private) 비밀번호 검증
     * @param password 비밀번호
     * @param confirmPassword 비밀번호 확인
     * @throws IllegalStateException 비밀번호가 일치하지 않을 경우 예외 발생
     */
    private void validatePassword(String password, String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            throw new IllegalStateException("비밀번호가 일치하지 않습니다.");
        }
    }
}

package com.goorm.clonestagram.user.application.service.profile;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.service.UserInternalQueryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProfileSelecterService {
    private final UserInternalQueryService userInternalQueryService;

    /**
     * (public) 사용자 프로필 조회
     *
     * @param userId 조회할 사용자의 ID
     * @return 사용자 프로필 정보
     * @throws IllegalArgumentException 사용자가 존재하지 않으면 예외 발생
     */
    public User getUserProfile(Long userId) {
        return userInternalQueryService.findByIdAndDeletedIsFalse(userId);
    }
}



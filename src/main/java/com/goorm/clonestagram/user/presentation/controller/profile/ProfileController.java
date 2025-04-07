package com.goorm.clonestagram.user.presentation.controller.profile;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import com.goorm.clonestagram.user.application.adapter.UserAdapter;
import com.goorm.clonestagram.user.application.dto.profile.UserProfileDto;
import com.goorm.clonestagram.user.application.dto.profile.UserProfileUpdateDto;
import com.goorm.clonestagram.user.application.service.profile.ProfileDeletionService;
import com.goorm.clonestagram.user.application.service.profile.ProfileSelecterService;
import com.goorm.clonestagram.user.application.service.profile.ProfileUpdaterService;
import com.goorm.clonestagram.user.domain.entity.User;

import lombok.RequiredArgsConstructor;

/**
 * ProfileController는 사용자의 프로필 정보 조회 및 수정 기능을 처리하는 API입니다.
 * 이 컨트롤러는 클라이언트의 요청에 따라 사용자의 프로필을 조회하거나 수정하는 작업을 수행합니다.
 *
 * 주요 메서드:
 * 1. 프로필 조회: 특정 사용자의 프로필 정보를 조회합니다.
 * 2. 프로필 수정: 사용자가 자신의 프로필 정보를 수정할 수 있게 합니다.
 */
@RestController
@RequiredArgsConstructor
public class ProfileController {
    private final ProfileSelecterService profileSelecterService;
    private final ProfileUpdaterService profileUpdaterService;
    private final ProfileDeletionService profileDeletionService;

    /**
     * 프로필 조회 API
     * - 주어진 사용자 ID를 기반으로 해당 사용자의 프로필 정보를 조회하는 기능을 제공합니다.
     *
     * @param userId 조회할 사용자의 고유 ID입니다. URL 경로 변수로 전달됩니다.
     * @return 사용자의 프로필 정보를 담은 UserProfileDto 객체를 반환합니다.
     */
    @GetMapping("/{userId}/profile")
    public ResponseEntity<UserProfileDto> getUserProfile(@PathVariable("userId") Long userId) {
        User user = profileSelecterService.getUserProfile(userId);
        return ResponseEntity.ok(UserAdapter.toUserProfileDto(user));
    }

    /**
     * 프로필 수정 API
     * - 사용자가 자신의 프로필 정보를 수정할 수 있는 기능을 제공합니다.
     *
     * @param userId 수정할 사용자의 고유 ID입니다. URL 경로 변수로 전달됩니다.
     * @param userProfileUpdateDto 사용자가 수정하고자 하는 프로필 정보를 담은 DTO입니다.
     *        이 DTO는 클라이언트로부터 전달됩니다.
     * @return 수정된 사용자 정보를 담은 User 엔티티 객체를 반환합니다.
     */
    @PutMapping(path = "/{userId}/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserProfileDto> updateUserProfile(@PathVariable("userId") Long userId,
                                                   UserProfileUpdateDto userProfileUpdateDto) {
        UserProfileDto userProfileDto = profileUpdaterService.updateUserProfile(userId, userProfileUpdateDto);
        return ResponseEntity.ok(userProfileDto);
    }

    /**
     * 프로필 삭제 API
     * - 사용자가 자신의 프로필을 삭제하는 기능을 제공합니다.
     *
     * @param userId 삭제할 사용자의 고유 ID입니다. URL 경로 변수로 전달됩니다.
     * @return HTTP 상태 코드 204 (NO CONTENT)를 반환합니다.
     */
    @DeleteMapping(path = "/{userId}/profile")
    public ResponseEntity<?> deleteUserProfile(@PathVariable("userId") Long userId){
        profileDeletionService.deleteUserProfile(userId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}

package com.goorm.clonestagram.user.controller;

import com.goorm.clonestagram.user.dto.UserProfileDto;
import com.goorm.clonestagram.user.dto.UserProfileUpdateDto;
import com.goorm.clonestagram.user.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ProfileController는 사용자의 프로필 정보 조회 및 수정 기능을 처리하는 API입니다.
 * 이 컨트롤러는 클라이언트의 요청에 따라 사용자의 프로필을 조회하거나 수정하는 작업을 수행합니다.
 *
 * 주요 메서드:
 * 1. 프로필 조회: 특정 사용자의 프로필 정보를 조회합니다.
 * 2. 프로필 수정: 사용자가 자신의 프로필 정보를 수정할 수 있게 합니다.
 */
@RestController
@RequiredArgsConstructor // 생성자를 자동으로 생성하여 의존성 주입을 처리합니다.
public class ProfileController {

    private final ProfileService profileService; // ProfileService를 통해 실제 비즈니스 로직을 수행합니다.

    /**
     * 프로필 조회 API
     * - 주어진 사용자 ID를 기반으로 해당 사용자의 프로필 정보를 조회하는 기능을 제공합니다.
     *
     * @param userId 조회할 사용자의 고유 ID입니다. URL 경로 변수로 전달됩니다.
     * @return 사용자의 프로필 정보를 담은 UserProfileDto 객체를 반환합니다.
     *
     * 이 API는 클라이언트로부터 특정 사용자의 프로필 정보를 요청받으면,
     * ProfileService의 getUserProfile 메서드를 호출하여 데이터를 가져온 후,
     * 응답으로 전달합니다.
     */
    @GetMapping("/{userId}/profile")  // {userId}는 URL 경로에 포함되어 전달된 사용자 ID입니다.
    public ResponseEntity<UserProfileDto> getUserProfile(@PathVariable Long userId) {
        // userId를 이용해 해당 사용자의 프로필을 조회합니다.
        UserProfileDto userProfileDto = profileService.getUserProfile(userId);

        // 조회된 프로필 정보를 클라이언트에게 전달합니다. ResponseEntity는 HTTP 응답을 감싸는 객체입니다.
        return ResponseEntity.ok(userProfileDto);  // HTTP 200 OK 상태로 반환합니다.
    }

    /**
     * 프로필 수정 API
     * - 사용자가 자신의 프로필 정보를 수정할 수 있는 기능을 제공합니다.
     *
     * @param userId 수정할 사용자의 고유 ID입니다. URL 경로 변수로 전달됩니다.
     * @param userProfileUpdateDto 사용자가 수정하고자 하는 프로필 정보를 담은 DTO입니다.
     *        이 DTO는 클라이언트로부터 전달됩니다.
     * @return 수정된 사용자 정보를 담은 User 엔티티 객체를 반환합니다.
     *
     * 이 API는 사용자가 자신의 프로필을 수정하려는 요청을 처리하며,
     * ProfileService의 updateUserProfile 메서드를 호출하여 해당 사용자의 프로필을 수정합니다.
     * 수정된 사용자 정보를 클라이언트에게 반환합니다.
     */
    @PutMapping(path = "/{userId}/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)  // {userId}는 URL 경로에 포함되어 수정할 사용자 ID를 전달합니다.
    public ResponseEntity<UserProfileDto> updateUserProfile(@PathVariable Long userId,
                                                   UserProfileUpdateDto userProfileUpdateDto) {
        // 사용자가 입력한 수정 정보를 기반으로 프로필을 업데이트합니다.
        UserProfileDto userProfileDto = profileService.updateUserProfile(userId, userProfileUpdateDto);

        // 수정된 사용자 정보를 클라이언트에게 전달합니다.
        return ResponseEntity.ok(userProfileDto);  // HTTP 200 OK 상태로 수정된 정보를 반환합니다.
    }

    @DeleteMapping(path = "/{userId}/profile")  // {userId}는 URL 경로에 포함되어 수정할 사용자 ID를 전달합니다.
    public ResponseEntity<?> deleteUserProfile(@PathVariable Long userId){
        profileService.deleteUserProfile(userId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}

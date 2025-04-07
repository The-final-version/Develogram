package com.goorm.clonestagram.user.application.dto.profile;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 프로필 조회를 위한 DTO
 * 프로필에 필요한 개인정보를 모두 반환
 */
@Getter
@Builder
@AllArgsConstructor
public class UserProfileDto {
    private Long id;
    private String username;
    private String userEmail;
    private String profileImgUrl;
    private String profileBio;
    private int followerCount;
    private int followingCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}

package com.goorm.clonestagram.user.dto;

import com.goorm.clonestagram.post.dto.PostInfoDto;
import com.goorm.clonestagram.user.domain.Users;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

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
    private String email;
    private String bio;
    private int followerCount;
    private int followingCount;
    private String profileimg;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserProfileDto fromEntity(Users users) {
        return UserProfileDto.builder()
                .id(users.getId())
                .username(users.getUsername())
                .email(users.getEmail())
                .profileimg(users.getProfileimg())
                .bio(users.getBio())
                .createdAt(users.getCreatedAt())
                .updatedAt(users.getUpdatedAt())
                .build();
    }
}

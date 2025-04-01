package com.goorm.clonestagram.follow.dto;

import com.goorm.clonestagram.user.domain.Users;
import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor
@Builder
@Getter
@Setter
@AllArgsConstructor
public class FollowDto {
    private Long id;               // 팔로우 관계 ID
    private Long followerId;       // 팔로우 하는 유저 ID
    private Long followedId;         // 팔로우 받는 유저 ID
    private String followerName;   // 팔로우 하는 유저 이름
    private String followedName;     // 팔로우 받는 유저 이름
    private String followerProfileimg; // 팔로우 하는 유저 프로필 이미지
    private String followedProfileImg;   // 팔로우 받는 유저 프로필 이미지
    private LocalDateTime createdAt; // 팔로우 생성 시간

    public FollowDto(Long id, Users follower, Users followed, LocalDateTime createdAt, String followerName, String followedName, String followerProfileimg, String followedProfileImg) {
        this.id = id;
        this.followerId = follower.getId();
        this.followedId = followed.getId();
        this.createdAt = createdAt;
        this.followerName = follower.getUsername(); // 팔로우 하는 유저 이름
        this.followedName = followed.getUsername();     // 팔로우 받는 유저 이름
        this.followerProfileimg = follower.getProfileimg(); // 팔로우 하는 유저 프로필 이미지
        this.followedProfileImg = followed.getProfileimg();   // 팔로우 받는 유저 프로필 이미지
    }

}

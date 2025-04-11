package com.goorm.clonestagram.follow.dto;

import com.goorm.clonestagram.user.domain.entity.User;

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

	// ↓ 유저 도메인 수정
	public FollowDto(Long id, User follower, User followed, LocalDateTime createdAt, String followerName,
		String followedName, String followerProfileimg, String followedProfileImg) {
		this.id = id;
		this.followerId = follower.getId();
		this.followedId = followed.getId();
		this.createdAt = createdAt;
		this.followerName = follower.getName(); // 팔로우 하는 유저 이름
		this.followedName = followed.getName();     // 팔로우 받는 유저 이름
		this.followerProfileimg = follower.getProfile().getImgUrl(); // 팔로우 하는 유저 프로필 이미지
		this.followedProfileImg = followed.getProfile().getImgUrl();   // 팔로우 받는 유저 프로필 이미지
	}

}

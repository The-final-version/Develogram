package com.goorm.clonestagram.user.application.dto.profile;

import com.goorm.clonestagram.user.domain.vo.ProfileBio;
import com.goorm.clonestagram.user.domain.vo.ProfileImageUrl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 프로필 수정에 필요한 데이터를 담고 있는 DTO 클래스
 * - 사용자가 프로필을 수정할 때 요청으로 전달되는 데이터를 관리
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateDto {
	private Long id;
	private ProfileBio bio;
	private ProfileImageUrl profileImage;



}

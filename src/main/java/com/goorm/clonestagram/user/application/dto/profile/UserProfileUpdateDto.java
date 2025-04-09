package com.goorm.clonestagram.user.application.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
	private String bio;
	private String profileImage;

}

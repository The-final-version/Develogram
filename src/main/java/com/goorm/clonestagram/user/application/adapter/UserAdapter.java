package com.goorm.clonestagram.user.application.adapter;

import org.springframework.data.domain.Page;

import com.goorm.clonestagram.user.application.dto.auth.JoinDto;
import com.goorm.clonestagram.user.application.dto.profile.UserProfileDto;
import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;

public class UserAdapter {

	/**
	 * JoinDto를 User 엔티티로 변환합니다.
	 *
	 * @param joinDto 회원가입 요청 DTO
	 * @return 변환된 Users 엔티티
	 */
	public static User fromUserProfileDto(JoinDto joinDto) {
		if (joinDto == null) {
			return null;
		}
		return new User(
			joinDto.getEmail(),
			joinDto.getPassword(),
			joinDto.getUsername()
		);
	}

	/**
	 * User 엔티티를 UserProfileDto로 변환합니다.
	 *
	 * @param user Users 엔티티
	 * @return 변환된 UserProfileDto
	 */
	public static UserProfileDto toUserProfileDto(User user) {
		if (user == null) {
			return null;
		}

		UserProfileDto.UserProfileDtoBuilder builder = UserProfileDto.builder()
			.id(user.getId())
			.name(user.getName())
			.userEmail(user.getEmail());

		if (user.getProfile() != null) {
			builder.profileImgUrl(user.getProfile().getImgUrl());
			builder.profileBio(user.getProfile().getBio());
		}

		return builder.build();
	}

	/**
	 * UserEntit를 UserProfileDto로 변환합니다.
	 *
	 * @param userEntity Users 엔티티
	 * @return 변환된 UserProfileDto
	 */
	public static UserProfileDto toUserProfileDto(UserEntity userEntity) {
		if (userEntity == null) {
			return null;
		}

		UserProfileDto.UserProfileDtoBuilder builder = UserProfileDto.builder()
			.id(userEntity.getId())
			.name(userEntity.getName())
			.userEmail(userEntity.getEmail());

		if (userEntity.getProfileEntity() != null) {
			builder.profileImgUrl(userEntity.getProfileEntity().getImgUrl());
			builder.profileBio(userEntity.getProfileEntity().getBio());
		}

		return builder.build();
	}

	/**
	 * Page<UserEntity>를 Page<UserProfileDto>로 변환합니다.
	 *
	 * @param usersPage Users 엔티티 페이지
	 * @return 변환된 UserProfileDto 페이지
	 */
	public static Page<UserProfileDto> toUserEntityProfileDtoPage(Page<UserEntity> usersPage) {
		return usersPage.map(UserAdapter::toUserProfileDto);
	}

}

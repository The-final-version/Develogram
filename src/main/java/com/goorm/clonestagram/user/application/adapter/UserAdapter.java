package com.goorm.clonestagram.user.application.adapter;

import java.util.List;

import org.springframework.data.domain.Page;

import com.goorm.clonestagram.user.application.dto.auth.JoinDto;
import com.goorm.clonestagram.user.application.dto.profile.UserProfileDto;
import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.vo.UserEmail;
import com.goorm.clonestagram.user.domain.vo.UserName;
import com.goorm.clonestagram.user.domain.vo.UserPassword;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;

public class UserAdapter {

	/**
	 * JoinDto를 Users 엔티티로 변환합니다.
	 *
	 * @param joinDto 회원가입 요청 DTO
	 * @return 변환된 Users 엔티티
	 */
	public static User fromUserProfileDto(JoinDto joinDto) {
		if (joinDto == null) {
			return null;
		}
		if (!joinDto.getPassword().equals(joinDto.getConfirmPassword())) {
			throw new IllegalStateException("비밀번호가 일치하지 않습니다.");
		}
		return new User(
			new UserEmail(joinDto.getEmail()),
			new UserPassword(joinDto.getPassword()),
			new UserName(joinDto.getUsername())
		);
	}

	/**
	 * Users 엔티티를 UserProfileDto로 변환합니다.
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
			.username(user.getName())
			.userEmail(user.getEmail());

		if (user.getProfile() != null) {
			builder.profileImgUrl(user.getProfile().getImgUrl());
			builder.profileBio(user.getProfile().getBio());
		}

		return builder.build();
	}

	/**
	 * UsersEntit를 UserProfileDto로 변환합니다.
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
			.username(userEntity.getName())
			.userEmail(userEntity.getEmail());

		if (userEntity.getProfileEntity() != null) {
			builder.profileImgUrl(userEntity.getProfileEntity().getImgUrl());
			builder.profileBio(userEntity.getProfileEntity().getBio());
		}

		return builder.build();
	}

	/**
	 * Page<Users>를 Page<UserProfileDto>로 변환합니다.
	 *
	 * @param usersPage Users 엔티티 페이지
	 * @return 변환된 UserProfileDto 페이지
	 */
	public static Page<UserProfileDto> toUserProfileDtoPage(Page<User> usersPage) {
		return usersPage.map(UserAdapter::toUserProfileDto);
	}

}

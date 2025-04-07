package com.goorm.clonestagram.user.domain.entity;

import com.goorm.clonestagram.user.domain.vo.ProfileBio;
import com.goorm.clonestagram.user.domain.vo.ProfileImageUrl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Profile 클래스
 */
@Getter
@Builder
@AllArgsConstructor
public class Profile {
	private ProfileImageUrl imgUrl;
	private ProfileBio bio;

	/**
	 * 프로필 이미지 URL을 업데이트하는 메서드
	 * @param newProfileImg 새로운 프로필 이미지 URL
	 * @return 업데이트된 Profile 객체
	 */
	public Profile updateProfileImage(ProfileImageUrl newProfileImg) {
		return new Profile(newProfileImg, this.bio);
	}

	/**
	 * 자기소개를 업데이트하는 메서드
	 * @param newProfileBio 새로운 자기소개
	 * @return 업데이트된 Profile 객체
	 */
	public Profile updateBio(ProfileBio newProfileBio) {
		return new Profile(this.imgUrl, newProfileBio);
	}

	public String getImgUrl() {
		return String.valueOf(this.imgUrl);
	}

	public String getBio() {
		return String.valueOf(this.bio);
	}

}

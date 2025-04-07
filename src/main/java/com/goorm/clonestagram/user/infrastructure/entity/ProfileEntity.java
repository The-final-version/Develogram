package com.goorm.clonestagram.user.infrastructure.entity;

import java.util.List;

import com.goorm.clonestagram.follow.domain.Follows;
import com.goorm.clonestagram.like.domain.Like;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.user.domain.entity.Profile;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Profile 클래스
 */
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProfileEntity {

	@Column(name = "profile_img_url")
	private String imgUrl;

	@Column(name = "bio")
	private String bio;

	@OneToMany(mappedBy = "followed", cascade = CascadeType.REMOVE, orphanRemoval = true)
	private List<Follows> following;  // 내가 팔로우한 리스트

	@OneToMany(mappedBy = "follower", cascade = CascadeType.REMOVE, orphanRemoval = true)
	private List<Follows> followers;  // 나를 팔로우한 리스트

	@OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true)
	private List<Like> likes;

	@OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true)
	private List<Posts> posts;

	public ProfileEntity(String imgUrl, String bio) {
		this.imgUrl = imgUrl;
		this.bio = bio;
	}

	public ProfileEntity(Profile profile) {
		this.imgUrl = profile.getImgUrl();
		this.bio = profile.getBio();
	}

	/**
	 * 프로필 이미지 URL을 업데이트하는 메서드
	 * @param newProfileImg 새로운 프로필 이미지 URL
	 * @return 업데이트된 Profile 객체
	 */
	public ProfileEntity updateProfileImage(String newProfileImg) {
		return new ProfileEntity(newProfileImg, this.bio);
	}

	/**
	 * 자기소개를 업데이트하는 메서드
	 * @param newProfileBio 새로운 자기소개
	 * @return 업데이트된 Profile 객체
	 */
	public ProfileEntity updateBio(String newProfileBio) {
		return new ProfileEntity(this.imgUrl, newProfileBio);
	}

	public String getImgUrl() {
		return String.valueOf(this.imgUrl);
	}

	public String getBio() {
		return String.valueOf(this.bio);
	}

}

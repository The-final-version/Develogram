package com.goorm.clonestagram.user.domain.entity;

import java.time.LocalDateTime;

import com.goorm.clonestagram.user.domain.vo.ProfileBio;
import com.goorm.clonestagram.user.domain.vo.ProfileImageUrl;
import com.goorm.clonestagram.user.domain.vo.UserEmail;
import com.goorm.clonestagram.user.domain.vo.UserName;
import com.goorm.clonestagram.user.domain.vo.UserPassword;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * User 엔티티
 * - 회원가입, 로그인, 프로필 수정 기능을 포함
 */
@Getter
@Builder
@AllArgsConstructor
public class User {
	private Long id;
	private UserEmail email;
	private UserPassword password;
	private UserName name;
	private Profile profile;
	private String createdBy;
	private String modifiedBy;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private Boolean deleted = false;
	private LocalDateTime deletedAt;

	public User(UserEmail email, UserPassword password, UserName name) {
		this.email = email;
		this.password = password;
		this.name = name;
	}

	@Builder
	public User(Long id, String email, String password, String username, ProfileBio profileBio, ProfileImageUrl profileImgUrl) {
		this.id = id;
		this.email = new UserEmail(email);
		this.password = new UserPassword(password);
		this.name = new UserName(username);
		this.profile = new Profile(profileImgUrl, profileBio);
	}

	public void updateProfile(Profile newProfile) {
		this.profile = newProfile;
	}

	/**
	 * 비밀번호 인증
	 *
	 * @param password 입력된 비밀번호
	 * @return 비밀번호가 일치하면 true, 그렇지 않으면 false
	 */
	public boolean authenticate(String password) {
		return this.password.matches(password);
	}

	public String getName() {
		return this.name.getName();
	}

	public String getEmail() {
		return this.email.getEmail();
	}

	public String getPassword() {
		return this.password.getHashedPassword();
	}

	public void delete() {
		this.deleted = true;
		this.deletedAt = LocalDateTime.now();
	}
}

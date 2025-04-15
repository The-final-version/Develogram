package com.goorm.clonestagram.user.domain.entity;

import java.time.LocalDateTime;

import com.goorm.clonestagram.user.domain.vo.UserPassword;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * User 엔티티
 * - 회원가입, 로그인, 프로필 수정 기능을 포함
 */
@Slf4j
@Getter
public class User {
	private Long id;
	private String email;
	private UserPassword password;
	private String name;
	private Profile profile;
	private String createdBy;
	private String modifiedBy;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private Boolean deleted = false;
	private LocalDateTime deletedAt;

	public User(String email, String password, String name) {
		this.email = email;
		this.password = new UserPassword(password); // 암호화 수행
		this.name = name;
	}

	public User(Long id) {
		this.id = id;
	}

	@Builder(builderMethodName = "secureBuilder")
	public static User secureBuild(
		Long id,
		String email,
		String password,
		boolean isHashed,
		String name,
		String profileBio,
		String profileImgUrl
	) {
		return new User(
			id,
			email,
			isHashed ? UserPassword.fromHashed(password) : new UserPassword(password),
			name,
			new Profile(profileImgUrl, profileBio),
			null, null, null, null, false, null
		);
	}

	private User(Long id, String email, UserPassword password, String name,
		Profile profile, String createdBy, String modifiedBy,
		LocalDateTime createdAt, LocalDateTime updatedAt,
		Boolean deleted, LocalDateTime deletedAt) {
		this.id = id;
		this.email = email;
		this.password = password;
		this.name = name;
		this.profile = profile;
		this.createdBy = createdBy;
		this.modifiedBy = modifiedBy;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.deleted = deleted;
		this.deletedAt = deletedAt;
	}

	public void updateProfile(Profile newProfile) {
		this.profile = newProfile;
	}

	public boolean authenticate(String password) {
		log.info("User.authenticate() called with password: {}", password);
		return this.password.matches(password);
	}

	public void delete() {
		this.deleted = true;
		this.deletedAt = LocalDateTime.now();
	}

	// 도메인 User
	public String getPassword() {
		return (this.password != null)
			? this.password.getHashedPassword()
			: "";
	}

	public static User withId(Long id) {
		return new User(id);
	}


	// ------------------------------------------------
	//  테스트에서 간단히 쓸 수 있는 목킹용 메서드
	// ------------------------------------------------
	public static User testMockUser(Long id, String name) {
		return User.secureBuilder()
			.id(id)
			.email(name + "@example.com")
			.password("mock1234@!")
			.isHashed(false)
			.name(name)
			.profileBio("This is a test bio for " + name)
			.profileImgUrl("https://example.com/mock_" + name + ".jpg")
			.build();
	}

	public static User testMockUser(String name) {
		return User.secureBuilder()
			.email(name + "@example.com")
			.password("mock1234!@")
			.isHashed(false)
			.name(name)
			.profileBio("This is a test bio for " + name)
			.profileImgUrl("https://example.com/mock_" + name + ".jpg")
			.build();
	}
}

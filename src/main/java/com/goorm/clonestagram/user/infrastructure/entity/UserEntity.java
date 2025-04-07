package com.goorm.clonestagram.user.infrastructure.entity;

import com.goorm.clonestagram.common.base.BaseEntity;
import com.goorm.clonestagram.user.domain.entity.Profile;
import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.vo.ProfileBio;
import com.goorm.clonestagram.user.domain.vo.ProfileImageUrl;
import com.goorm.clonestagram.user.domain.vo.UserEmail;
import com.goorm.clonestagram.user.domain.vo.UserName;
import com.goorm.clonestagram.user.domain.vo.UserPassword;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * User 엔티티
 * - 회원가입, 로그인, 프로필 수정 기능을 포함
 */

@Entity
@Getter
@AllArgsConstructor
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "email", nullable = true)
	private String email;

	@Column(name = "password", nullable = true)
	private String password;

	@Column(name = "name", nullable = true)
	private String name;

	@Embedded
	private ProfileEntity profileEntity;

	public UserEntity(String email, String password, String name) {
		this.email = email;
		this.password = password;
		this.name = name;
	}

	@Builder
	public UserEntity(Long id, String email, String password, String username, String profileBio, String profileImgUrl) {
		this.id = id;
		this.email = email;
		this.password = password;
		this.name = username;
		this.profileEntity = new ProfileEntity(profileImgUrl, profileBio);
	}

	public void updateProfile(ProfileEntity newProfileEntity) {
		this.profileEntity = newProfileEntity;
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


	/**
	 * 도메인 Users 객체로 변환합니다.
	 * (프로필 관련 값은 필요에 따라 추가 변환)
	 */
	public User toDomain() {
		Profile profile;
		if (this.profileEntity != null) {
			profile = new Profile(
				new ProfileImageUrl(this.profileEntity.getImgUrl()),
				new ProfileBio(this.profileEntity.getBio())
			);
		}
		else {
			profile = new Profile(
				new ProfileImageUrl(""),
				new ProfileBio("")
			);
		}
		return User.builder()
			.id(this.id)
			.email(new UserEmail(this.email))
			.password(UserPassword.fromHashed(this.password))
			.name(new UserName(this.name))
			.profile(profile)
			.build();
	}

	/**
	 * 도메인 Users 객체를 기반으로 UsersEntity를 생성합니다.
	 */
	public UserEntity(User user) {
		Profile profile;
		if (this.profileEntity != null) {
			profile = new Profile(
				new ProfileImageUrl(this.profileEntity.getImgUrl()),
				new ProfileBio(this.profileEntity.getBio())
			);
		}
		else {
			profile = new Profile(
				new ProfileImageUrl(""),
				new ProfileBio("")
			);
		}
		this.id = user.getId();
		this.email = user.getEmail();
		this.password = user.getPassword();
		this.name = user.getName();
		this.profileEntity = new ProfileEntity(
			profile
		);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof UserEntity)) return false;
		UserEntity other = (UserEntity) o;
		// "id != null" 가 전제(영속화된 엔티티)일 때만 의미 있음
		return this.id != null && this.id.equals(other.id);
	}

	@Override
	public int hashCode() {
		// id가 null이면 0, 아니면 id의 hash
		return (this.id == null) ? 0 : this.id.hashCode();
	}

	public UserEntity(Long id) {
		this.id = id;
	}
}

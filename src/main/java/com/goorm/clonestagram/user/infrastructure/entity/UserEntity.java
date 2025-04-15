package com.goorm.clonestagram.user.infrastructure.entity;

import com.goorm.clonestagram.common.base.BaseEntity;
import com.goorm.clonestagram.user.domain.entity.Profile;
import com.goorm.clonestagram.user.domain.entity.User;

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

	@Column(name = "email", unique = true)
	private String email;

	@Column(name = "password")
	private String password;

	@Column(name = "name")
	private String name;

	@Embedded
	private ProfileEntity profileEntity;

	public UserEntity(String email, String password, String name) {
		this.email = email;
		this.password = password;
		this.name = name;
	}

	@Builder
	public UserEntity(Long id, String email, String password, String name, String profileBio, String profileImgUrl) {
		this.id = id;
		this.email = email;
		this.password = password;
		this.name = name;
		this.profileEntity = new ProfileEntity(profileImgUrl, profileBio);
	}

	public void updateProfile(ProfileEntity newProfileEntity) {
		this.profileEntity = newProfileEntity;
	}

	/**
	 * 도메인 User 객체를 UserEntity로 변환합니다.
	 *
	 * @param user 도메인 사용자 객체
	 * @return 변환된 UserEntity 인스턴스
	 */
	public static UserEntity from(User user) {
		String bio = "";
		String imgUrl = "";
		if (user.getProfile() != null) {
			bio = user.getProfile().getBio() == null ? "" : user.getProfile().getBio();
			imgUrl = user.getProfile().getImgUrl() == null ? "" : user.getProfile().getImgUrl();
		}
		return UserEntity.builder()
			.id(user.getId())
			.email(user.getEmail())
			.password(user.getPassword())
			.name(user.getName())
			.profileBio(bio)
			.profileImgUrl(imgUrl)
			.build();
	}

	/**
	 * 도메인 Users 객체로 변환합니다.
	 * (프로필 관련 값은 필요에 따라 추가 변환)
	 */
	public User toDomain() {
		Profile profile;
		if (this.profileEntity != null) {
			profile = new Profile(
				this.profileEntity.getImgUrl(),
				this.profileEntity.getBio()
			);
		}
		else {
			profile = new Profile(
				"",
				""
			);
		}
		return User.secureBuilder()
			.id(this.id)
			.email(this.email)
			.password(this.password)
			.isHashed(true) // 저장된 password는 이미 해시된 값이므로 true
			.name(this.name)
			.profileBio(profile.getBio())
			.profileImgUrl(profile.getImgUrl())
			.build();
	}

	/**
	 * 도메인 Users 객체를 기반으로 UsersEntity를 생성합니다.
	 */
	public UserEntity(User user) {
		Profile profile;
		if (this.profileEntity != null) {
			profile = new Profile(
				this.profileEntity.getImgUrl(),
				this.profileEntity.getBio()
			);
		}
		else {
			profile = new Profile(
				"",
				""
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
		return (this.id == null) ? 0 : this.id.hashCode();
	}

	public UserEntity(Long id) {
		this.id = id;
	}

	public void setName(String s) {
		this.name = s;
	}

}

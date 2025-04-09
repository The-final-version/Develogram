package com.goorm.clonestagram.user.application.service.profile;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goorm.clonestagram.user.application.adapter.UserAdapter;
import com.goorm.clonestagram.user.application.dto.profile.UserProfileDto;
import com.goorm.clonestagram.user.application.dto.profile.UserProfileUpdateDto;
import com.goorm.clonestagram.user.domain.entity.Profile;
import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.service.UserInternalQueryService;
import com.goorm.clonestagram.user.domain.vo.ProfileBio;
import com.goorm.clonestagram.user.domain.vo.ProfileImageUrl;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProfileUpdaterService {
	private final UserInternalQueryService userInternalQueryService;

	/**
	 * 사용자 프로필 수정
	 * @param userId 수정할 사용자의 ID
	 * @param userProfileUpdateDto 수정할 프로필 정보
	 * @return 수정된 사용자 객체
	 * @throws IllegalArgumentException 사용자가 존재하지 않으면 예외 발생
	 */
	@Transactional
	public UserProfileDto updateUserProfile(Long userId, UserProfileUpdateDto userProfileUpdateDto) {
		// DB에서 사용자 정보를 조회 (존재하지 않으면 예외 발생)
		User user = userInternalQueryService.findByIdAndDeletedIsFalse(userId);

		// 업데이트된 프로필 정보를 생성하여 사용자에 반영
		Profile updatedProfileEntity = update(user.getProfile(), userProfileUpdateDto);
		user.updateProfile(updatedProfileEntity);
		userInternalQueryService.saveUser(user);

		// 업데이트된 사용자 정보를 DTO로 변환하여 반환
		return UserAdapter.toUserProfileDto(user);
	}


	/* =============================
       ( Private Methods )
   	============================= */

	/**
	 * 프로필 업데이트
	 * @param currentProfile 현재 프로필
	 * @param dto 업데이트할 프로필 정보
	 * @return 업데이트된 프로필
	 */
	private Profile update(Profile currentProfile, UserProfileUpdateDto dto) {
		// 자기소개(bio) 업데이트
		currentProfile = updateBio(currentProfile, new ProfileBio(dto.getBio()));

		// 프로필 이미지 URL 업데이트
		currentProfile = updateProfileImage(currentProfile, new ProfileImageUrl(dto.getProfileImage()));

		return currentProfile;
	}

	/**
	 * 프로필 자기소개(bio) 업데이트
	 * @param currentProfile 현재 프로필
	 * @param bio 업데이트할 자기소개(bio)
	 * @return 업데이트된 프로필
	 */
	private Profile updateBio(Profile currentProfile, ProfileBio bio) {
		if (bio != null && !bio.getBio().isEmpty()) {
			return currentProfile.updateBio(bio);
		}
		return currentProfile;
	}

	/**
	 * 프로필 이미지 URL 업데이트
	 * @param currentProfile 현재 프로필
	 * @param profileImageUrl 업데이트할 프로필 이미지 URL
	 * @return 업데이트된 프로필
	 */
	private Profile updateProfileImage(Profile currentProfile, ProfileImageUrl profileImageUrl) {
		if (profileImageUrl != null && !profileImageUrl.getUrl().isEmpty()) {
			try {
				return currentProfile.updateProfileImage(profileImageUrl);
			} catch (Exception e) {
				throw new RuntimeException("프로필 이미지 업로드 실패: " + e.getMessage());
			}
		}
		return currentProfile;
	}
}

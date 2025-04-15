package com.goorm.clonestagram.user.application.service.profile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.goorm.clonestagram.user.application.adapter.UserAdapter;
import com.goorm.clonestagram.user.application.dto.profile.UserProfileDto;
import com.goorm.clonestagram.user.application.dto.profile.UserProfileUpdateDto;
import com.goorm.clonestagram.user.domain.entity.Profile;
import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.service.UserInternalQueryService;
import com.goorm.clonestagram.user.domain.vo.ProfileBio;
import com.goorm.clonestagram.user.domain.vo.ProfileImageUrl;

@ExtendWith(MockitoExtension.class)
class ProfileUpdaterServiceTest {

	@Mock
	private UserInternalQueryService userInternalQueryService;

	@InjectMocks
	private ProfileUpdaterService profileUpdaterService;

	@Mock
	private User user;

	@Mock
	private Profile profile;

	@Test
	@DisplayName("정상 업데이트: bio와 profileImage가 모두 제공된 경우")
	void updateUserProfile_Success() {
		// given
		Long userId = 1L;
		String newBio = "New bio";
		String newProfileImage = "http://image.url";
		UserProfileUpdateDto updateDto = UserProfileUpdateDto.builder()
			.bio(new ProfileBio(newBio))
			.profileImage(new ProfileImageUrl(newProfileImage))
			.build();

		when(userInternalQueryService.findByIdAndDeletedIsFalse(userId)).thenReturn(user);
		when(user.getProfile()).thenReturn(profile);
		// bio 업데이트 시 profile.updateBio()가 호출되어 동일 Profile 객체를 리턴하도록 설정
		when(profile.updateBio(any(ProfileBio.class))).thenReturn(profile);
		// profileImage 업데이트 시 profile.updateProfileImage()가 호출되어 동일 Profile 객체를 리턴하도록 설정
		when(profile.updateProfileImage(any(ProfileImageUrl.class))).thenReturn(profile);

		// UserAdapter.toUserProfileDto()는 static 메서드이므로 mockStatic으로 스텁 처리
		UserProfileDto expectedDto = new UserProfileDto();
		try (MockedStatic<UserAdapter> mockedAdapter = mockStatic(UserAdapter.class)) {
			mockedAdapter.when(() -> UserAdapter.toUserProfileDto(user)).thenReturn(expectedDto);

			// when
			UserProfileDto result = profileUpdaterService.updateUserProfile(userId, updateDto);

			// then
			verify(userInternalQueryService).findByIdAndDeletedIsFalse(userId);
			verify(user).getProfile();
			verify(profile).updateBio(any(ProfileBio.class));
			verify(profile).updateProfileImage(any(ProfileImageUrl.class));
			verify(user).updateProfile(profile);
			verify(userInternalQueryService).saveUser(user);
			assertEquals(expectedDto, result);
		}
	}

	@Test
	@DisplayName("프로필 이미지 업데이트 실패 시: 예외 발생")
	void updateUserProfile_ProfileImageUploadFailure() {
		// given
		Long userId = 1L;
		String newBio = "New bio";
		String failingProfileImage = "http://fail.url";
		UserProfileUpdateDto updateDto = UserProfileUpdateDto.builder()
			.bio(new ProfileBio(newBio))
			.profileImage(new ProfileImageUrl(failingProfileImage))
			.build();

		when(userInternalQueryService.findByIdAndDeletedIsFalse(userId)).thenReturn(user);
		when(user.getProfile()).thenReturn(profile);
		when(profile.updateBio(any(ProfileBio.class))).thenReturn(profile);
		// 프로필 이미지 업데이트 시 예외 발생하도록 설정
		when(profile.updateProfileImage(any(ProfileImageUrl.class)))
			.thenThrow(new RuntimeException("File upload error"));

		// when & then
		RuntimeException exception = assertThrows(RuntimeException.class,
			() -> profileUpdaterService.updateUserProfile(userId, updateDto));
		assertTrue(exception.getMessage().contains("프로필 이미지 업로드 실패:"));
		verify(userInternalQueryService).findByIdAndDeletedIsFalse(userId);
		verify(user).getProfile();
		verify(profile).updateBio(any(ProfileBio.class));
		verify(profile).updateProfileImage(any(ProfileImageUrl.class));
		// 예외 발생 시 updateProfile()와 saveUser() 호출이 이뤄지지 않아야 함
		verify(user, never()).updateProfile(any());
		verify(userInternalQueryService, never()).saveUser(any());
	}

	@Test
	@DisplayName("사용자 미존재 시: 예외 발생")
	void updateUserProfile_UserNotFound() {
		// given
		Long userId = 1L;
		UserProfileUpdateDto updateDto = UserProfileUpdateDto.builder()
			.bio(new ProfileBio("New bio"))
			.profileImage(new ProfileImageUrl("http://image.url"))
			.build();

		// 사용자 조회 시 IllegalArgumentException 발생하도록 설정 (또는 해당 예외를 던지도록 구현)
		when(userInternalQueryService.findByIdAndDeletedIsFalse(userId))
			.thenThrow(new IllegalArgumentException("User not found"));

		// when & then
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
			() -> profileUpdaterService.updateUserProfile(userId, updateDto));
		assertEquals("User not found", exception.getMessage());
		verify(userInternalQueryService).findByIdAndDeletedIsFalse(userId);
		verify(user, never()).getProfile();
		verify(userInternalQueryService, never()).saveUser(any());
	}
}

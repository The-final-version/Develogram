package com.goorm.clonestagram.user.presentation.controller.profile;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goorm.clonestagram.user.application.adapter.UserAdapter;
import com.goorm.clonestagram.user.application.dto.profile.UserProfileDto;
import com.goorm.clonestagram.user.application.dto.profile.UserProfileUpdateDto;
import com.goorm.clonestagram.user.application.service.profile.ProfileDeletionService;
import com.goorm.clonestagram.user.application.service.profile.ProfileSelecterService;
import com.goorm.clonestagram.user.application.service.profile.ProfileUpdaterService;
import com.goorm.clonestagram.user.domain.entity.Profile;
import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.vo.ProfileBio;
import com.goorm.clonestagram.user.domain.vo.ProfileImageUrl;

@WebMvcTest(controllers = ProfileController.class)
@ContextConfiguration(classes = {ProfileController.class, ProfileControllerTest.TestConfig.class})
@AutoConfigureMockMvc(addFilters = false) // 보안 필터를 비활성화하여 인증/CSRF 문제를 우회
class ProfileControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper; // 필요 시 사용

	@Autowired
	private ProfileSelecterService profileSelecterService;

	@Autowired
	private ProfileUpdaterService profileUpdaterService;

	@Autowired
	private ProfileDeletionService profileDeletionService;

	@TestConfiguration
	static class TestConfig {
		@Bean
		public ProfileSelecterService profileSelecterService() {
			return Mockito.mock(ProfileSelecterService.class);
		}
		@Bean
		public ProfileUpdaterService profileUpdaterService() {
			return Mockito.mock(ProfileUpdaterService.class);
		}
		@Bean
		public ProfileDeletionService profileDeletionService() {
			return Mockito.mock(ProfileDeletionService.class);
		}
	}

	@Nested
	@DisplayName("프로필 조회 API 테스트")
	class GetUserProfileTest {
		@Test
		@DisplayName("정상적으로 프로필 조회 시 200과 프로필 DTO 반환")
		void getUserProfileSuccess() throws Exception {
			Long userId = 1L;

			// 테스트용 Profile 및 User 객체 생성
			User user = User.secureBuilder()
				.id(userId)
				.name("홍길동")
				.email("test@example.com")
				.password("hashedpassword1!")
				.isHashed(false)
				.profileBio("안녕하세요!")
				.profileImgUrl("http://example.com/img.jpg")
				.build();

			// 서비스 모의 동작 설정
			when(profileSelecterService.getUserProfile(eq(userId))).thenReturn(user);

			// 어댑터를 통해 DTO 생성
			UserProfileDto expectedDto = UserAdapter.toUserProfileDto(user);

			// GET 요청 수행 및 검증
			mockMvc.perform(get("/{userId}/profile", userId)
					.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(expectedDto.getId()))
				.andExpect(jsonPath("$.name").value(expectedDto.getName()))
				.andExpect(jsonPath("$.userEmail").value(expectedDto.getUserEmail()))
				.andExpect(jsonPath("$.profileImgUrl").value(expectedDto.getProfileImgUrl()))
				.andExpect(jsonPath("$.profileBio").value(expectedDto.getProfileBio()));
		}
	}

	@Nested
	@DisplayName("프로필 수정 API 테스트")
	class UpdateUserProfileTest {
		@Test
		@DisplayName("정상적으로 프로필 수정 시 200과 수정된 프로필 DTO 반환")
		void updateUserProfileSuccess() throws Exception {
			Long userId = 1L;
			String newBio = "새로운 소개글";
			String newProfileImage = "http://example.com/new-img.jpg";

			UserProfileUpdateDto updateDto = UserProfileUpdateDto.builder()
				.id(userId)
				.bio(new ProfileBio(newBio))
				.profileImage(new ProfileImageUrl(newProfileImage))
				.build();

			// 예상 응답 DTO 생성
			UserProfileDto responseDto = UserProfileDto.builder()
				.id(userId)
				.name("홍길동")
				.userEmail("test@example.com")
				.profileImgUrl(newProfileImage)
				.profileBio(newBio)
				.followerCount(10)
				.followingCount(5)
				.createdAt(LocalDateTime.now().minusDays(1))
				.updatedAt(LocalDateTime.now())
				.build();

			when(profileUpdaterService.updateUserProfile(eq(userId), any(UserProfileUpdateDto.class)))
				.thenReturn(responseDto);

			// multipart PUT 요청 구성 (PUT 방식으로 오버라이드)
			MockMultipartFile bioPart = new MockMultipartFile("bio", "", "text/plain", newBio.getBytes());
			MockMultipartFile profileImagePart = new MockMultipartFile("profileImage", "", "text/plain", newProfileImage.getBytes());

			mockMvc.perform(multipart("/{userId}/profile", userId)
					.file(bioPart)
					.file(profileImagePart)
					.with(request -> {
						request.setMethod("PUT");
						return request;
					})
					.contentType(MediaType.MULTIPART_FORM_DATA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(userId))
				.andExpect(jsonPath("$.profileBio").value(newBio))
				.andExpect(jsonPath("$.profileImgUrl").value(newProfileImage));
		}
	}

	@Nested
	@DisplayName("프로필 삭제 API 테스트")
	class DeleteUserProfileTest {
		@Test
		@DisplayName("프로필 삭제 시 204 응답 반환")
		void deleteUserProfileSuccess() throws Exception {
			Long userId = 1L;
			doNothing().when(profileDeletionService).deleteUserProfile(eq(userId));

			mockMvc.perform(delete("/{userId}/profile", userId))
				.andExpect(status().isNoContent());
		}
	}
}

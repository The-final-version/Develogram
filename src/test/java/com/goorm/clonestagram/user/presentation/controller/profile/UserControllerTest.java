package com.goorm.clonestagram.user.presentation.controller.profile;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goorm.clonestagram.user.application.adapter.UserAdapter;
import com.goorm.clonestagram.user.application.dto.profile.UserProfileDto;
import com.goorm.clonestagram.user.domain.entity.Profile;
import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.service.UserInternalQueryService;
import com.goorm.clonestagram.user.domain.vo.ProfileBio;
import com.goorm.clonestagram.user.domain.vo.ProfileImageUrl;
import com.goorm.clonestagram.user.domain.vo.UserEmail;
import com.goorm.clonestagram.user.domain.vo.UserName;
import com.goorm.clonestagram.user.domain.vo.UserPassword;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;
import com.goorm.clonestagram.util.CustomUserDetails;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@WebMvcTest(UserController.class)
@ContextConfiguration(classes = {UserController.class, UserControllerTest.MockServiceConfig.class})
class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserInternalQueryService userQueryService;

	@TestConfiguration
	static class MockServiceConfig {
		@Bean
		public UserInternalQueryService userInternalQueryService() {
			return Mockito.mock(UserInternalQueryService.class);
		}
	}

	@Nested
	@DisplayName("내 프로필 조회 (/user/me)")
	class GetMyProfileTest {

		@Test
		@DisplayName("로그인된 유저일 경우 프로필 정보 반환")
		void getMyProfileSuccess() throws Exception {
			// given
			Long userId = 1L;
			User user = User.builder()
				.id(userId)
				.name(new UserName("홍길동"))
				.email(new UserEmail("test@example.com"))
				.password(new UserPassword("hashedpassword1!"))
				.profile(Profile.builder()
					.bio(new ProfileBio("소개"))
					.imgUrl(new ProfileImageUrl("http://img.com/profile.jpg"))
					.build())
				.build();

			// CustomUserDetails에 UserEntity를 wrapping하여 전달
			CustomUserDetails userDetails = new CustomUserDetails(new UserEntity(user));
			UserProfileDto expectedDto = UserAdapter.toUserProfileDto(user);

			// when & then : 인증된 사용자로 요청
			mockMvc.perform(get("/user/me")
					.with(SecurityMockMvcRequestPostProcessors.user(userDetails)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(expectedDto.getId()))
				.andExpect(jsonPath("$.username").value(expectedDto.getUsername()))
				.andExpect(jsonPath("$.userEmail").value(expectedDto.getUserEmail()));
		}

		@Test
		@DisplayName("인증정보 없을 경우 401 반환")
		void getMyProfileUnauthorized() throws Exception {
			mockMvc.perform(get("/user/me"))
				.andExpect(status().isUnauthorized());
		}
	}

	@Nested
	@DisplayName("username으로 유저 ID 조회 (/user/id)")
	class GetUserIdByUsernameTest {

		@Test
		@DisplayName("정상적으로 유저 ID 반환")
		void getUserIdByUsernameSuccess() throws Exception {
			String username = "홍길동";
			Long expectedId = 1L;

			when(userQueryService.findUserIdByUsername(username)).thenReturn(expectedId);

			User dummyUser = User.builder()
				.id(expectedId)
				.name(new UserName(username))
				.email(new UserEmail("test@example.com"))
				.password(new UserPassword("hashedpassword1!"))
				.profile(Profile.builder()
					.bio(new ProfileBio("소개"))
					.imgUrl(new ProfileImageUrl("http://img.com/profile.jpg"))
					.build())
				.build();
			CustomUserDetails userDetails = new CustomUserDetails(new UserEntity(dummyUser));

			mockMvc.perform(get("/user/id")
					.param("username", username)
					.with(SecurityMockMvcRequestPostProcessors.user(userDetails)))
				.andExpect(status().isOk())
				.andExpect(content().string(expectedId.toString()));
		}
	}
}

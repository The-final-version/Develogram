package com.goorm.clonestagram.user.presentation.controller.auth;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goorm.clonestagram.exception.user.ErrorCode;
import com.goorm.clonestagram.exception.user.error.InvalidCredentialsException;
import com.goorm.clonestagram.user.application.dto.auth.LoginForm;
import com.goorm.clonestagram.user.application.dto.auth.LoginResponseDto;
import com.goorm.clonestagram.user.application.service.auth.UserLoginService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import com.goorm.clonestagram.common.exception.GlobalExceptionHandler;

@WebMvcTest(controllers = LoginController.class,
	// 데이터소스, JPA, 보안 자동 구성을 제외합니다.
	excludeAutoConfiguration = {
		DataSourceAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class,
		SecurityAutoConfiguration.class
	})
@AutoConfigureMockMvc(addFilters = false)  // 보안 필터 비활성화
@ContextConfiguration(classes = {LoginController.class, LoginControllerTest.TestConfig.class})
@Import(GlobalExceptionHandler.class)
class LoginControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private UserLoginService userLoginService;

	private static final String VALID_EMAIL = "test11@example.com";
	private static final String VALID_PASSWORD = "validPassword1!";
	@TestConfiguration
	static class TestConfig {
		@Bean
		public UserLoginService userLoginService() {
			return org.mockito.Mockito.mock(UserLoginService.class);
		}
	}

	@Nested
	@DisplayName("정상 로그인 케이스")
	class LoginSuccess {
		@Test
		@DisplayName("유효한 로그인 정보 제공 시 200 응답과 성공 응답 반환")
		void login_Success() throws Exception {
			// given: 유효한 로그인 요청 DTO 생성
			LoginForm loginDto = new LoginForm(VALID_EMAIL, VALID_PASSWORD);

			// 예상되는 로그인 성공 응답 DTO
			LoginResponseDto responseDto = LoginResponseDto.builder()
				.message("로그인 성공")
				.userId("123")
				.accessToken("ACCESS_TOKEN")
				.refreshToken("REFRESH_TOKEN")
				.build();

			when(userLoginService.loginAndBuildResponse(eq(VALID_EMAIL), eq(VALID_PASSWORD),
				any(),"device28282"))
				.thenReturn(responseDto);

			// when & then: POST /login 호출 시 200 응답과 JSON 응답 내용 검증
			mockMvc.perform(post("/login")
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(loginDto)))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("로그인 성공"))
				.andExpect(jsonPath("$.userId").value("123"))
				.andExpect(jsonPath("$.accessToken").value("ACCESS_TOKEN"))
				.andExpect(jsonPath("$.refreshToken").value("REFRESH_TOKEN"));
		}
	}

	@Nested
	@DisplayName("로그인 실패 케이스")
	class LoginFailure {
		@Test
		@DisplayName("잘못된 로그인 정보 제공 시 401 응답")
		void login_Failure_BadCredentials() throws Exception {
			LoginForm dto = new LoginForm(VALID_EMAIL, "wrongPassword");

			when(userLoginService.loginAndBuildResponse(eq(VALID_EMAIL), eq("wrongPassword"), any(),"device999"))
				.thenThrow(new InvalidCredentialsException());

			mockMvc.perform(post("/login").with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(dto)))
				.andDo(print())
				.andExpect(status().isUnauthorized())
				.andExpect(content().string(ErrorCode.AUTH_INVALID_CREDENTIALS.getMessage()));
		}
	}

	@Nested
	@DisplayName("입력 검증 실패 케이스")
	class ValidationFailure {
		@Test
		@DisplayName("이메일이 빈 문자열인 경우 400 응답")
		void login_BlankEmail() throws Exception {

			LoginForm loginDto = new LoginForm("", VALID_PASSWORD);

			mockMvc.perform(post("/login")
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(loginDto)))
				.andDo(print())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorDetail[0]")
					.value(org.hamcrest.Matchers.containsString(
						ErrorCode.USER_EMAIL_BLANK.getMessage())));
		}

		@Test
		@DisplayName("비밀번호가 빈 문자열인 경우 400 응답")
		void login_BlankPassword() throws Exception {
			// 빈 비밀번호 입력 시 Bean Validation에 의해 400 발생
			LoginForm loginDto = new LoginForm(VALID_EMAIL, "");

			mockMvc.perform(post("/login")
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(loginDto)))
				.andDo(print())
				.andExpect(status().isBadRequest());
		}
	}
}

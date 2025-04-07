package com.goorm.clonestagram.user.presentation.controller.auth;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goorm.clonestagram.user.application.dto.auth.JoinDto;
import com.goorm.clonestagram.user.application.service.auth.UserJoinService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(controllers = JoinController.class,
	excludeAutoConfiguration = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@ContextConfiguration(classes = {JoinController.class, JoinControllerTest.TestConfig.class})
class JoinControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserJoinService userJoinService;

	@Configuration
	static class TestConfig {
		@Bean
		public UserJoinService userJoinService() {
			return org.mockito.Mockito.mock(UserJoinService.class);
		}
	}

	@Test
	@DisplayName("정상 가입: 유효한 입력이면 201 응답과 '회원가입 성공' 메시지 반환")
	void join_Success() throws Exception {
		// given: 유효한 JoinDto 생성
		JoinDto dto = JoinDto.builder()
			.email("valid@test.com")
			.password("mypassword")
			.confirmPassword("mypassword")
			.username("홍길동")
			.build();

		doNothing().when(userJoinService).joinProcess(dto);

		// when & then
		mockMvc.perform(post("/join")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(dto)))
			.andExpect(status().isCreated())
			.andExpect(content().string("회원가입 성공"));
	}

	@Nested
	@DisplayName("입력 검증 실패 케이스")
	class ValidationErrorCases {

		@Test
		@DisplayName("이메일 공백 시 -> 400 및 '이메일을 입력해주세요.' 에러")
		void join_BlankEmail() throws Exception {
			JoinDto dto = JoinDto.builder()
				.email("")
				.password("validPassword")
				.confirmPassword("validPassword")
				.username("홍길동")
				.build();

			mockMvc.perform(post("/join")
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(dto)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$[0].defaultMessage").value("이메일을 입력해주세요."));
		}

		@Test
		@DisplayName("유효하지 않은 이메일 형식 시 -> 400 및 '유효한 이메일을 입력해주세요.' 에러")
		void join_InvalidEmailFormat() throws Exception {
			JoinDto dto = JoinDto.builder()
				.email("invalid-email")
				.password("validPassword")
				.confirmPassword("validPassword")
				.username("홍길동")
				.build();

			mockMvc.perform(post("/join")
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(dto)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$[0].defaultMessage").value("유효한 이메일을 입력해주세요."));
		}

		@Test
		@DisplayName("비밀번호 공백 시 -> 400 및 '비밀번호는 6자 이상이어야 합니다.' 에러")
		void join_BlankPassword() throws Exception {
			// given: 가상 데이터 생성 및 stubbing
			JoinDto dto = JoinDto.builder()
				.email("test@test.com")
				.password("")
				.confirmPassword("validPassword")
				.username("홍길동")
				.build();

			// when & then: join API 호출 후 에러 메시지 검증
			mockMvc.perform(post("/join")
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(dto)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$[0].defaultMessage").value("비밀번호는 6자 이상이어야 합니다."));
		}


		@Test
		@DisplayName("비밀번호 길이 6자 미만 시 -> 400 및 '비밀번호는 6자 이상이어야 합니다.' 에러")
		void join_ShortPassword() throws Exception {
			JoinDto dto = JoinDto.builder()
				.email("test@test.com")
				.password("12345")
				.confirmPassword("12345")
				.username("홍길동")
				.build();

			mockMvc.perform(post("/join")
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(dto)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$[0].defaultMessage").value("비밀번호는 6자 이상이어야 합니다."));
		}

		@Test
		@DisplayName("비밀번호 길이 20자 초과 시 -> 400 및 '비밀번호는 20자 이하이어야 합니다.' 에러")
		void join_LongPassword() throws Exception {
			String longPassword = "abcdefghijklmnopqrstu"; // 21자
			JoinDto dto = JoinDto.builder()
				.email("test@test.com")
				.password(longPassword)
				.confirmPassword(longPassword)
				.username("홍길동")
				.build();

			mockMvc.perform(post("/join")
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(dto)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$[0].defaultMessage").value("비밀번호는 20자 이하이어야 합니다."));
		}

		@Test
		@DisplayName("비밀번호 확인 공백 시 -> 400 및 '비밀번호 확인을 입력해주세요.' 에러")
		void join_BlankConfirmPassword() throws Exception {
			JoinDto dto = JoinDto.builder()
				.email("test@test.com")
				.password("validPassword")
				.confirmPassword("")
				.username("홍길동")
				.build();

			mockMvc.perform(post("/join")
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(dto)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$[0].defaultMessage").value("비밀번호 재입력을 입력해주세요."));
		}

		@Test
		@DisplayName("이름 공백 시 -> 400 및 '이름을 입력해주세요.' 에러")
		void join_BlankUsername() throws Exception {
			JoinDto dto = JoinDto.builder()
				.email("test@test.com")
				.password("validPassword")
				.confirmPassword("validPassword")
				.username("")
				.build();

			mockMvc.perform(post("/join")
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(dto)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$[?(@.defaultMessage=='이름을 입력해주세요.')]").exists());
		}
	}

	@Test
	@DisplayName("회원가입 실패: 서비스 예외(이메일 중복) 발생 시 400 응답 및 예외 메시지 반환")
	void join_ServiceException() throws Exception {
		JoinDto dto = JoinDto.builder()
			.email("duplicate@test.com")
			.password("mypassword")
			.confirmPassword("mypassword")
			.username("홍길동")
			.build();

		doThrow(new IllegalStateException("이미 사용 중인 이메일입니다. email = duplicate@test.com"))
			.when(userJoinService).joinProcess(Mockito.any(JoinDto.class));

		mockMvc.perform(post("/join")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(dto)))
			.andExpect(status().isBadRequest())
			.andExpect(content().string("이미 사용 중인 이메일입니다. email = duplicate@test.com"));
	}
}

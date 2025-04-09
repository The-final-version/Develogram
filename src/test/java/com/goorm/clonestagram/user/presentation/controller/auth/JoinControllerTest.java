package com.goorm.clonestagram.user.presentation.controller.auth;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goorm.clonestagram.common.exception.GlobalExceptionHandler;
import com.goorm.clonestagram.exception.user.ErrorCode;
import com.goorm.clonestagram.exception.user.error.DuplicateEmailException;
import com.goorm.clonestagram.user.application.dto.auth.JoinDto;
import com.goorm.clonestagram.user.application.dto.auth.LoginForm;
import com.goorm.clonestagram.user.application.service.auth.UserJoinService;
import com.goorm.clonestagram.user.domain.service.UserInternalQueryService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(controllers = JoinController.class,
	excludeAutoConfiguration = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@ContextConfiguration(classes = {JoinController.class, JoinControllerTest.TestConfig.class})
@Import(GlobalExceptionHandler.class)
class JoinControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private UserJoinService userJoinService;

	@Configuration
	static class TestConfig {
		@Bean
		public UserJoinService userJoinService() {
			return org.mockito.Mockito.mock(UserJoinService.class);
		}
	}

	@BeforeEach
	void setUp() {
		Mockito.reset(userJoinService);
	}

	@Test
	@DisplayName("정상 가입: 유효한 입력이면 201 응답과 '회원가입 성공' 메시지 반환")
	void join_Success() throws Exception {
		// given
		JoinDto dto = new JoinDto("valid@test.com", "mypassword11@", "mypassword11@", "홍길동");
		doNothing().when(userJoinService).joinProcess(dto);

		// when & then
		mockMvc.perform(post("/join")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(dto)))
			.andDo(print())
			.andExpect(status().isCreated())
			.andExpect(content().string("회원가입 성공"));
	}

	@Test
	@DisplayName("회원가입 실패: 이메일 중복 → 409 & 메시지")
	void join_DuplicateEmail() throws Exception {
		// given
		JoinDto dto = new JoinDto("dup@test.com","pw","pw","홍길동");
		doThrow(new DuplicateEmailException())
			.when(userJoinService).joinProcess(any(JoinDto.class));

		// when & then
		mockMvc.perform(post("/join").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(dto)))
			.andDo(print())
			.andExpect(status().isConflict())                              // 409
			.andExpect(content().string(ErrorCode.USER_DUPLICATE_EMAIL.getMessage()));
	}

}

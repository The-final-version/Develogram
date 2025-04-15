package com.goorm.clonestagram.user.presentation.controller.auth;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goorm.clonestagram.common.exception.BusinessException;
import com.goorm.clonestagram.common.exception.ErrorCode;
import com.goorm.clonestagram.common.exception.GlobalExceptionHandler;
import com.goorm.clonestagram.user.application.dto.auth.JoinDto;
import com.goorm.clonestagram.user.application.service.auth.UserJoinService;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(controllers = JoinController.class,
	excludeAutoConfiguration = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@ContextConfiguration(classes = {JoinController.class, JoinControllerTest.class})
@Import(GlobalExceptionHandler.class)
class JoinControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private UserJoinService userJoinService;

	@Test
	@DisplayName("정상 가입: 유효한 입력이면 201 응답과 '회원가입 성공' 메시지 반환")
	void join_Success() throws Exception {
		// given
		JoinDto dto = new JoinDto("valid@test.com", "mypassword11@", "mypassword11@", "홍길동");  // Ensure name is not null or empty
		doNothing().when(userJoinService).joinProcess(dto);

		// when & then
		mockMvc.perform(MockMvcRequestBuilders.post("/join")
				.content(objectMapper.writeValueAsString(dto))
				.contentType(MediaType.APPLICATION_JSON)
			)
			.andDo(MockMvcResultHandlers.print())
			.andExpect(status().isCreated());
	}

	@Test
	@DisplayName("회원가입 실패: 이메일 중복 → 메시지")
	void join_DuplicateEmail() throws Exception {
		// given
		JoinDto dto = new JoinDto("dup@test.com","pw","pw","홍길동");
		doThrow(new BusinessException(ErrorCode.USER_DUPLICATE_EMAIL))
			.when(userJoinService).joinProcess(any(JoinDto.class));

		// when & then
		mockMvc.perform(MockMvcRequestBuilders.post("/join")
			.content(objectMapper.writeValueAsString(dto))
			.contentType(MediaType.APPLICATION_JSON))
			.andDo(MockMvcResultHandlers.print())
			.andExpect(jsonPath("$.errorMessage")
				.value(ErrorCode.USER_DUPLICATE_EMAIL.getMessage()));
	}

}

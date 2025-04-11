package com.goorm.clonestagram.user.presentation.controller.auth;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goorm.clonestagram.common.exception.ErrorCode;
import com.goorm.clonestagram.common.exception.GlobalExceptionHandler;
import com.goorm.clonestagram.common.jwt.JwtTokenProvider;
import com.goorm.clonestagram.user.application.dto.auth.LoginForm;
import com.goorm.clonestagram.user.application.service.auth.UserLoginService;
import com.goorm.clonestagram.user.domain.service.UserInternalQueryService;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;

@SpringBootTest
@AutoConfigureMockMvc
@Import(GlobalExceptionHandler.class)
class LoginControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserLoginService userLoginService;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private UserInternalQueryService userInternalQueryService;

	@MockitoBean
	private UserEntity userEntity;

	private static final String VALID_EMAIL = "test11@example.com";
	private static final String VALID_PASSWORD = "validPassword1!";


}

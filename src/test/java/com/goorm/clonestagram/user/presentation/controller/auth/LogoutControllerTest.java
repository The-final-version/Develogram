package com.goorm.clonestagram.user.presentation.controller.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.goorm.clonestagram.user.application.service.auth.UserLogoutService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc(addFilters = false)
@SpringBootTest
@Import(LogoutControllerTest.TestConfig.class)
class LogoutControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserLogoutService logoutService;

	@TestConfiguration
	static class TestConfig {
		@Bean
		public UserLogoutService userLogoutService() {
			return Mockito.mock(UserLogoutService.class);
		}
	}

	@Test
	void logoutSuccess() throws Exception {
		doNothing().when(logoutService).logout(any());

		mockMvc.perform(post("/api/auth/logout"))
			.andExpect(status().isOk())
			.andExpect(content().string("로그아웃 성공"));
	}
}

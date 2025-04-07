package com.goorm.clonestagram.user.application.service.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class UserLogoutServiceTest {

	@InjectMocks
	private UserLogoutService userLogoutService;

	@Mock
	private HttpServletRequest request;

	@Mock
	private HttpSession session;

	@AfterEach
	void tearDown() {
		// 테스트 후 SecurityContextHolder 초기화
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("로그아웃: 세션이 존재하는 경우, 세션 무효화 및 SecurityContextHolder 초기화")
	void testLogoutWithSession() {
		// given: 세션이 존재하는 경우
		when(request.getSession(false)).thenReturn(session);
		// SecurityContextHolder에 임의의 인증 정보가 존재하는 상황
		SecurityContextHolder.getContext().setAuthentication(mock(org.springframework.security.core.Authentication.class));

		// when
		userLogoutService.logout(request);

		// then
		verify(session, times(1)).invalidate();
		assertNull(SecurityContextHolder.getContext().getAuthentication(), "SecurityContextHolder가 초기화되어야 함");
	}

	@Test
	@DisplayName("로그아웃: 세션이 존재하지 않는 경우, SecurityContextHolder 초기화만 수행")
	void testLogoutWithoutSession() {
		// given: 세션이 존재하지 않는 경우
		when(request.getSession(false)).thenReturn(null);
		SecurityContextHolder.getContext().setAuthentication(mock(org.springframework.security.core.Authentication.class));

		// when
		userLogoutService.logout(request);

		// then
		verify(session, never()).invalidate(); // 세션 무효화 호출 없이 SecurityContextHolder만 초기화
		assertNull(SecurityContextHolder.getContext().getAuthentication(), "SecurityContextHolder가 초기화되어야 함");
	}
}

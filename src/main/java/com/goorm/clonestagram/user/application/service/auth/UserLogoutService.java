package com.goorm.clonestagram.user.application.service.auth;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.goorm.clonestagram.common.jwt.LoginDeviceRegistry;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserLogoutService {

	/**
	 * (public) 로그아웃 처리
	 * - 세션 무효화 및 SecurityContextHolder 초기화
	 * @param request HTTP 요청 객체
	 */
	public void logout(HttpServletRequest request) {
		// 1) 세션이 존재하면 무효화
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}

		// 2) SecurityContextHolder 초기화
		SecurityContextHolder.clearContext();

		// 3) JWT 토큰의 기기 ID 삭제
		if (request.getUserPrincipal() != null) {
			String name = request.getUserPrincipal().getName();
			LoginDeviceRegistry.remove(name);
		}
	}
}

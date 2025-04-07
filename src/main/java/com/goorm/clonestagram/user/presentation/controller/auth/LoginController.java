package com.goorm.clonestagram.user.presentation.controller.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.goorm.clonestagram.user.application.dto.auth.LoginForm;
import com.goorm.clonestagram.user.application.service.auth.UserLoginService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class LoginController {
	private final UserLoginService userLoginService;

	@PostMapping("/login")
	public ResponseEntity<Object> login(@RequestBody LoginForm loginForm, HttpServletRequest request) {
		try {
			return ResponseEntity.ok(
				userLoginService.loginAndBuildResponse(
					loginForm.getEmail(),
					loginForm.getPassword(), request));
		} catch (
			BadCredentialsException e) {
			return new ResponseEntity<>("이메일 또는 비밀번호가 잘못되었습니다.", HttpStatus.UNAUTHORIZED);
		}
	}
}

package com.goorm.clonestagram.user.presentation.controller.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.goorm.clonestagram.user.application.dto.auth.LoginForm;
import com.goorm.clonestagram.user.application.dto.auth.LoginResponseDto;
import com.goorm.clonestagram.user.application.service.auth.UserLoginService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
public class LoginController {
	private final UserLoginService loginService;

	@PostMapping("/login")
	public ResponseEntity<Object> login(
		@Valid @RequestBody LoginForm form, HttpServletRequest req) {
		LoginResponseDto response = loginService.loginAndBuildResponse(
			form.getEmail(), form.getPassword(), req, req.getHeader("User-Agent"));
		log.debug("Login successful: {}", response);
		return ResponseEntity.ok(response);
	}
}

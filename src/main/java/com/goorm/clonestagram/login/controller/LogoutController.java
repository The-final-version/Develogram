package com.goorm.clonestagram.login.controller;

import com.goorm.clonestagram.login.service.LoginService;
import com.goorm.clonestagram.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class LogoutController {

    private final LoginService loginService;

    private final AuthenticationManager authenticationManager;

    private final UserRepository userRepository; // ✅ 유저 ID 조회용

    // UserController.java
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok("로그아웃 성공");
    }

}

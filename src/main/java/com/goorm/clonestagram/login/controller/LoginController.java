package com.goorm.clonestagram.login.controller;


import com.goorm.clonestagram.login.dto.LoginForm;
import com.goorm.clonestagram.login.dto.LoginResponseDto;
import com.goorm.clonestagram.login.service.LoginService;
import com.goorm.clonestagram.user.domain.Users;
import com.goorm.clonestagram.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class LoginController {

    private final LoginService loginService;

    private final AuthenticationManager authenticationManager;

    private final UserRepository userRepository; // ✅ 유저 ID 조회용

    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody LoginForm loginForm, HttpServletRequest request) {
        String email = loginForm.getEmail();
        String password = loginForm.getPassword();

        if (loginService.login(email, password)) {
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(email, password);
            Authentication authentication = authenticationManager.authenticate(authenticationToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            HttpSession session = request.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

            // ✅ 유저 ID 추출
            Users users = userRepository.findByEmail(email);
            String userId = users.getId().toString();

            // ✅ 응답 JSON 형태로 반환
            return ResponseEntity.ok(new LoginResponseDto("로그인 성공", userId));
        } else {
            return new ResponseEntity<>("이메일 또는 비밀번호가 잘못되었습니다.", HttpStatus.UNAUTHORIZED);
        }
    }
}

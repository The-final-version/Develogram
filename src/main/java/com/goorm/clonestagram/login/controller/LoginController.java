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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class LoginController {

    private final LoginService loginService;

    private final AuthenticationManager authenticationManager;

    private final UserRepository userRepository; // ✅ 유저 ID 조회용

    @PostMapping("/login")
    public ResponseEntity<Object> login(@ModelAttribute LoginForm loginForm, HttpServletRequest request) {
        String username = loginForm.getUsername();
        String password = loginForm.getPassword();

        if (loginService.login(username, password)) {
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(username, password);
            Authentication authentication = authenticationManager.authenticate(authenticationToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            HttpSession session = request.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

            Users users = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("해당 username을 찾을 수 없습니다."));
            String userId = users.getId().toString();

            return ResponseEntity.ok(new LoginResponseDto("로그인 성공", userId));
        } else {
            return new ResponseEntity<>("아이디 또는 비밀번호가 잘못되었습니다.", HttpStatus.UNAUTHORIZED);
        }
    }
}


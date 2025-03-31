package com.goorm.clonestagram.login;

import com.goorm.clonestagram.login.service.LoginService;
import com.goorm.clonestagram.user.domain.User;
import com.goorm.clonestagram.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class LoginServiceIntegrationTest {

    @Autowired private LoginService loginService;
    @Autowired private UserRepository userRepository;
    @Autowired private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("로그인성공")
    void 로그인_성공() {
        // given
        String rawPassword = "password123";
        User user = User.builder()
                .email("user@example.com")
                .username("홍길동")
                .password(passwordEncoder.encode(rawPassword))
                .build();
        userRepository.save(user);

        // when
        boolean result = loginService.login("user@example.com", rawPassword);

        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("비밀번호 불일치")
    void 비밀번호_불일치() {
        // given
        User user = User.builder()
                .email("user2@example.com")
                .username("이몽룡")
                .password(passwordEncoder.encode("correctPassword"))
                .build();
        userRepository.save(user);

        // when
        boolean result = loginService.login("user2@example.com", "wrongPassword");

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("존재하지 않는 이메일")
    void 존재하지_않는_이메일() {
        // given
        String email = "nonexistent@example.com";

        // when & then
        UsernameNotFoundException ex = assertThrows(UsernameNotFoundException.class, () ->
                loginService.login(email, "anyPassword"));
        assertEquals("사용자를 찾을 수 없습니다.", ex.getMessage());
    }
}

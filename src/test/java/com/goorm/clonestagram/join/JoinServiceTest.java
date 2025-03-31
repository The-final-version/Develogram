package com.goorm.clonestagram.join;

import com.goorm.clonestagram.login.dto.JoinDto;
import com.goorm.clonestagram.login.service.JoinService;
import com.goorm.clonestagram.user.domain.User;
import com.goorm.clonestagram.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class JoinServiceIntegrationTest {

    @Autowired private JoinService joinService;
    @Autowired private UserRepository userRepository;
    @Autowired private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void clean() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("회원가입 성공")
    void 회원가입_성공() {
        // given
        JoinDto dto = new JoinDto();
        dto.setEmail("test@example.com");
        dto.setPassword("password123");
        dto.setConfirmPassword("password123");
        dto.setName("홍길동");

        // when
        joinService.joinProcess(dto);

        // then
        User user = userRepository.findByEmail("test@example.com");
        assertNotNull(user);
        assertEquals("홍길동", user.getUsername());
        assertTrue(passwordEncoder.matches("password123", user.getPassword()));
    }

    @Test
    @DisplayName("이메일 중복 오류")
    void 이메일_중복_오류() {
        // given
        User existing = User.builder()
                .email("dup@example.com")
                .password("encodedpassword")
                .username("기존유저")
                .build();
        userRepository.save(existing);

        JoinDto dto = new JoinDto();
        dto.setEmail("dup@example.com");
        dto.setPassword("password123");
        dto.setConfirmPassword("password123");
        dto.setName("신규유저");

        // when & then
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> joinService.joinProcess(dto));
        assertEquals("이미 존재하는 이메일입니다.", ex.getMessage());
    }
    @Test
    @DisplayName("이름 누락으로 회원가입 실패")
    void 이름_누락으로_회원가입_실패() {
        // given: 이름이 빠진 DTO
        JoinDto dto = new JoinDto();
        dto.setEmail("no-name@example.com");
        dto.setPassword("password123");
        dto.setConfirmPassword("password123");
        dto.setName("");  // 이름 없음

        // when & then
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            joinService.joinProcess(dto);
        });
        assertEquals("이름을 입력해주세요.", ex.getMessage());
    }

    @Test
    @DisplayName("비밀번호 누락으로 회원가입 실패")
    void 비밀번호_누락으로_회원가입_실패() {
        // given: 비밀번호가 빈 문자열이거나 null
        JoinDto dto = new JoinDto();
        dto.setEmail("nopassword@example.com");
        dto.setPassword("");  // 누락된 비밀번호
        dto.setConfirmPassword("");  // 비어 있어도 로직상 같게 세팅됨
        dto.setName("비번없는유저");

        // when & then
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            joinService.joinProcess(dto);
        });
        assertEquals("비밀번호를 입력해주세요.", ex.getMessage());
    }

}

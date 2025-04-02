package com.goorm.clonestagram.post.service;

import com.goorm.clonestagram.user.domain.Users;
import com.goorm.clonestagram.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")  // <- 이게 있어야 test 환경으로 바뀜
public class ProfileServiceTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    public void setUp() {
        userRepository.deleteAll(); // 테스트 실행 전 기존 데이터 삭제
    }

    @Test
    public void updateUserProfile_Success() {
        // Given
        Users users = Users.builder()
                .username("testuser")  // 필수 필드 추가
                .password("password123")  // 필수 필드 추가
                .email("testuser@example.com")  // 필수 필드 추가
                .profileimg("http://example.com/profile.jpg")
                .bio("안녕하세요. 테스트 사용자입니다.")
                .build();

        // When
        Users savedUsers = userRepository.save(users);

        // Then
        assertNotNull(savedUsers.getId());
        assertEquals("testuser", savedUsers.getUsername());
        assertEquals("testuser@example.com", savedUsers.getEmail());
        assertEquals("http://example.com/profile.jpg", savedUsers.getProfileimg());
        assertEquals("안녕하세요. 테스트 사용자입니다.", savedUsers.getBio());
    }

    @Test
    public void getUserProfile_Success() {
        // Given
        Users users = Users.builder()
                .username("testuser2")
                .password("password456")
                .email("testuser2@example.com")
                .profileimg("http://example.com/profile2.jpg")
                .bio("두 번째 사용자입니다.")
                .build();
        userRepository.save(users);

        // When
        Users foundUsers = userRepository.findByIdAndDeletedIsFalse(users.getId()).orElse(null);

        // Then
        assertNotNull(foundUsers);
        assertEquals("testuser2", foundUsers.getUsername());
        assertEquals("testuser2@example.com", foundUsers.getEmail());
        assertEquals("http://example.com/profile2.jpg", foundUsers.getProfileimg());
        assertEquals("두 번째 사용자입니다.", foundUsers.getBio());
    }

    @Test
    public void updateProfileImage_Success() {
        // Given
        Users users = Users.builder()
                .username("testuser3")
                .password("password789")
                .email("testuser3@example.com")
                .profileimg("http://example.com/profile3.jpg")
                .bio("세 번째 사용자입니다.")
                .build();
        Users savedUsers = userRepository.save(users);

        // When
        savedUsers.setUsername("testuser4");
        savedUsers.setEmail("testuser4@example.com");
        savedUsers.setBio("update!");
        savedUsers.setProfileimg("http://example.com/newprofile3.jpg");
        Users updatedUsers = userRepository.save(savedUsers);

        // Then
        assertEquals("http://example.com/newprofile3.jpg", updatedUsers.getProfileimg());
        assertEquals("update!", updatedUsers.getBio());
        assertEquals("testuser4", updatedUsers.getUsername());
        assertEquals("testuser4@example.com", updatedUsers.getEmail());
    }
}
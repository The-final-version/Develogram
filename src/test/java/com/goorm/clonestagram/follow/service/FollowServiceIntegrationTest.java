package com.goorm.clonestagram.follow.service;

import com.goorm.clonestagram.follow.domain.Follows;
import com.goorm.clonestagram.follow.repository.FollowRepository;
import com.goorm.clonestagram.user.domain.User;
import com.goorm.clonestagram.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")  // <- 이게 있어야 test 환경으로 바뀜
@SpringBootTest
@Transactional
@ExtendWith(SpringExtension.class)
public class FollowServiceIntegrationTest {

    @Autowired
    private FollowService followService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FollowRepository followRepository;

    @Test
    @DisplayName("팔로우 통합 테스트: 실제 DB를 활용한 팔로우 토글 기능 검증")

    public void testToggleFollowIntegration() {
        // Given: 실제 DB에 유저 저장
        User user1 = new User();
        user1.setUsername("user1");
        user1.setEmail("user1@example.com");  // email은 nullable=false이므로 반드시 설정
        user1.setPassword("password1");       // password도 nullable=false
        user1.setProfileimg("profile1");
        user1 = userRepository.save(user1);

        User user2 = new User();
        user2.setUsername("user2");
        user2.setEmail("user2@example.com");
        user2.setPassword("password2");
        user2.setProfileimg("profile2");
        user2 = userRepository.save(user2);

        // When: 팔로우 토글
        followService.toggleFollow(user1.getId(), user2.getId());

        // Then: 실제 DB에서 확인
        Optional<Follows> follow = followRepository.findByFromUserAndToUser(user1, user2);
        assertTrue(follow.isPresent());
    }
}

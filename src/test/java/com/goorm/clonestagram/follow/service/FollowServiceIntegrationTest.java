package com.goorm.clonestagram.follow.service;

import com.goorm.clonestagram.follow.domain.Follows;
import com.goorm.clonestagram.follow.repository.FollowRepository;
import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;
import com.goorm.clonestagram.user.infrastructure.repository.JpaUserExternalWriteRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
    private JpaUserExternalWriteRepository userRepository;

    @Autowired
    private FollowRepository followRepository;
    UserEntity user1, user2;
    @BeforeEach
    public void setUp() {
        // 테스트 환경에서 DB 초기화
        followRepository.deleteAll();
        userRepository.deleteAll();
        user1 = new UserEntity(User.testMockUser("user1"));
        user2 = new UserEntity(User.testMockUser("user2"));
        user1 = userRepository.save(user1);
        user2 = userRepository.save(user2);
    }

    @Test
    public void testToggleFollowIntegration() {
        // Given: 실제 DB에 유저 저장

        // When: 팔로우 토글
        followService.toggleFollow(user1.getId(), user2.getId());

        // Then: 최신 엔티티를 다시 로드하여 검사
        user1 = userRepository.findById(user1.getId()).orElseThrow();
        user2 = userRepository.findById(user2.getId()).orElseThrow();

        // Then: 실제 DB에서 확인
        Optional<Follows> follow = followRepository.findByFollowerAndFollowedWithLock(user1, user2);
        assertTrue(follow.isPresent());
    }
}

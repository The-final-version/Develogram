package com.goorm.clonestagram.follow.service;

import com.goorm.clonestagram.follow.domain.Follows;
import com.goorm.clonestagram.follow.repository.FollowRepository;
import com.goorm.clonestagram.user.domain.repository.UserExternalWriteRepository;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;

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
    private UserExternalWriteRepository userRepository;

    @Autowired
    private FollowRepository followRepository;

    @Test
    public void testToggleFollowIntegration() {
        // Given: 실제 DB에 유저 저장
        UserEntity user1 = UserEntity.builder()
            .name("user1")
            .email("user1@example.com")
            .password("password1")
            .profileImgUrl("profile1")
            .profileBio("bio1").build();
        user1 = userRepository.save(user1);

        UserEntity user2 = UserEntity.builder()
            .name("user2")
            .email("user2@example.com")
            .password("password2")
            .profileImgUrl("profile2")
            .profileBio("bio2").build();
        user1 = userRepository.save(user1);
        user2 = userRepository.save(user2);

        // When: 팔로우 토글
        followService.toggleFollow(user1.getId(), user2.getId());

        // Then: 실제 DB에서 확인
        Optional<Follows> follow = followRepository.findByFollowerAndFollowed(user1, user2);
        assertTrue(follow.isPresent());
    }
}

package com.goorm.clonestagram.like.service;

import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.post.repository.PostsRepository;
import com.goorm.clonestagram.like.domain.Like;
import com.goorm.clonestagram.like.repository.LikeRepository;
import com.goorm.clonestagram.user.domain.User;
import com.goorm.clonestagram.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.goorm.clonestagram.post.ContentType.IMAGE;
import static com.goorm.clonestagram.post.ContentType.VIDEO;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")  // <- 이게 있어야 test 환경으로 바뀜
@SpringBootTest
@Transactional
public class LikeServiceIntegrationTest {

    @Autowired
    private LikeService likeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostsRepository postsRepository;

    @Autowired
    private LikeRepository likeRepository;

    @DisplayName("좋아요 토글 - 추가 및 취소 동작 검증")
    @Test
    public void testToggleLike() {
        // Given: 테스트용 사용자와 게시물 생성
        User user = new User();
        user.setUsername("testUser");
        user.setEmail("testlike@example.com");
        user.setPassword("password");
        user.setDeleted(false);
        user = userRepository.save(user);

        Posts post = new Posts();
        post.setUser(user);
        post.setContent("Test Post");
        post.setContentType(IMAGE);  // contents_type 추가
        post.setMediaName("test-url");   // media_url 추가 (nullable=false일 경우)
        post.setDeleted(false);
        post = postsRepository.save(post);

        // When: 좋아요 토글 (첫 번째 좋아요)
        likeService.toggleLike(user.getId(), post.getId());

        // Then: 좋아요가 제대로 생성되었는지 확인
        Optional<Like> like = likeRepository.findByUserIdAndPostsId(user.getId(), post.getId());
        assertTrue(like.isPresent(), "좋아요가 생성되어야 합니다.");
        assertEquals(1L, likeService.getLikeCount(post.getId()), "좋아요 개수가 1이어야 합니다.");

        // When: 좋아요 다시 토글 (좋아요 취소)
        likeService.toggleLike(user.getId(), post.getId());

        // Then: 좋아요가 제대로 취소되었는지 확인
        like = likeRepository.findByUserIdAndPostsId(user.getId(), post.getId());
        assertFalse(like.isPresent(), "좋아요가 취소되어야 합니다.");
        assertEquals(0L, likeService.getLikeCount(post.getId()), "좋아요 개수가 0이어야 합니다.");
    }

    @DisplayName("좋아요 개수 조회 - 다수 사용자 좋아요")
    @Test
    public void testGetLikeCount() {
        // Given: 테스트용 사용자와 게시물, 다중 좋아요 생성
        User user1 = new User();
        user1.setUsername("user1");
        user1.setEmail("user1@example.com");
        user1.setPassword("password1");
        user1.setDeleted(false);
        user1 = userRepository.save(user1);

        User user2 = new User();
        user2.setUsername("user2");
        user2.setEmail("user2@example.com");
        user2.setPassword("password2");
        user2.setDeleted(false);
        user2 = userRepository.save(user2);

        Posts post = new Posts();
        post.setUser(user1);
        post.setContent("Test Post1");
        post.setContentType(IMAGE);  // contents_type 추가
        post.setMediaName("test-url");   // media_url 추가 (nullable=false일 경우)
        post.setDeleted(false);
        post = postsRepository.save(post);

        // When: 두 사용자가 좋아요
        likeService.toggleLike(user1.getId(), post.getId());
        likeService.toggleLike(user2.getId(), post.getId());

        // Then: 좋아요 개수 확인
        assertEquals(2L, likeService.getLikeCount(post.getId()), "좋아요 개수가 2이어야 합니다.");
    }
}
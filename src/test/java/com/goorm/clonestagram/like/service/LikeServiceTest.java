package com.goorm.clonestagram.like.service;

import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.post.repository.PostsRepository;
import com.goorm.clonestagram.like.domain.Like;
import com.goorm.clonestagram.like.repository.LikeRepository;
import com.goorm.clonestagram.user.domain.User;
import com.goorm.clonestagram.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")  // <- 이게 있어야 test 환경으로 바뀜
public class LikeServiceTest {

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostsRepository postRepository;

    @InjectMocks
    private LikeService likeService;

    private User user;
    private Posts post;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Test user
        user = new User();
        user.setId(1L);
        user.setUsername("user1");

        // Test posts
        post = new Posts();
        post.setId(1L);
        post.setContent("Test Post");
    }

    @DisplayName("좋아요 토글 - 좋아요가 없을 때 추가")
    @Test
    public void testToggleLikeAddLike() {
        // Given: User and posts are available, and no existing like in the database.
        when(userRepository.findByIdAndDeletedIsFalse(1L)).thenReturn(Optional.of(user));
        when(postRepository.findByIdAndDeletedIsFalse(1L)).thenReturn(Optional.of(post));
        when(likeRepository.findByUserIdAndPostsId(1L, 1L)).thenReturn(Optional.empty());  // No like exists

        // When: User toggles like
        likeService.toggleLike(1L, 1L);

        // Then: The like should be saved
        verify(likeRepository, times(1)).save(any(Like.class));
        verify(likeRepository, times(0)).delete(any(Like.class));
    }

    @DisplayName("좋아요 토글 - 기존 좋아요 있을 때 삭제")
    @Test
    public void testToggleLikeRemoveLike() {
        // Given: User and posts are available, and existing like is found in the database.
        Like existingLike = new Like();
        existingLike.setUser(user);
        existingLike.setPosts(post);

        when(userRepository.findByIdAndDeletedIsFalse(1L)).thenReturn(Optional.of(user));
        when(postRepository.findByIdAndDeletedIsFalse(1L)).thenReturn(Optional.of(post));
        when(likeRepository.findByUserIdAndPostsId(1L, 1L)).thenReturn(Optional.of(existingLike));  // Like already exists

        // When: User toggles like
        likeService.toggleLike(1L, 1L);

        verify(likeRepository, times(0)).save(any(Like.class));
        verify(likeRepository, times(1)).delete(existingLike);
    }

    @DisplayName("좋아요 토글 - 사용자 존재하지 않을 경우 예외 발생")
    @Test
    void toggleLike_ShouldThrowException_WhenUserNotFound() {
        when(userRepository.findByIdAndDeletedIsFalse(1L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                likeService.toggleLike(1L, 10L)
        );

        assertEquals("사용자를 찾을 수 없습니다.", ex.getMessage());
    }

    @DisplayName("좋아요 토글 - 게시물 존재하지 않을 경우 예외 발생")
    @Test
    void toggleLike_ShouldThrowException_WhenPostNotFound() {
        when(userRepository.findByIdAndDeletedIsFalse(1L)).thenReturn(Optional.of(user));
        when(postRepository.findByIdAndDeletedIsFalse(10L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                likeService.toggleLike(1L, 10L)
        );

        assertEquals("게시물을 찾을 수 없습니다.", ex.getMessage());
    }

    @DisplayName("좋아요 개수 조회 - 정확한 수 반환")
    @Test
    void getLikeCount_ShouldReturnCorrectCount() {
        when(likeRepository.countByPostsId(10L)).thenReturn(3L);

        Long count = likeService.getLikeCount(10L);

        assertEquals(3L, count);
    }
}

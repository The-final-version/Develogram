package com.goorm.clonestagram.like.service;

import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.post.repository.PostsRepository;
import com.goorm.clonestagram.like.domain.Like;
import com.goorm.clonestagram.like.repository.LikeRepository;
import com.goorm.clonestagram.user.domain.Users;
import com.goorm.clonestagram.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

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

    private Users user;
    private Posts post;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Test user
        user = new Users();
        user.setId(1L);
        user.setUsername("user1");

        // Test posts
        post = new Posts();
        post.setId(1L);
        post.setContent("Test Post");
    }

    @Test
    public void testToggleLikeAddLike() {
        // Given: User and posts are available, and no existing like in the database.
        when(userRepository.findByIdAndDeletedIsFalse(1L)).thenReturn(Optional.of(user));
        when(postRepository.findByIdAndDeletedIsFalse(1L)).thenReturn(Optional.of(post));
        when(likeRepository.findByUser_IdAndPost_Id(1L, 1L)).thenReturn(Optional.empty());  // No like exists

        // When: User toggles like
        likeService.toggleLike(1L, 1L);

        // Then: The like should be saved
        verify(likeRepository, times(1)).save(any(Like.class));
        verify(likeRepository, times(0)).delete(any(Like.class));
    }

    @Test
    public void testToggleLikeRemoveLike() {
        // Given: User and posts are available, and existing like is found in the database.
        Like existingLike = new Like(user, post);

        when(userRepository.findByIdAndDeletedIsFalse(1L)).thenReturn(Optional.of(user));
        when(postRepository.findByIdAndDeletedIsFalse(1L)).thenReturn(Optional.of(post));
        when(likeRepository.findByUser_IdAndPost_Id(1L, 1L)).thenReturn(Optional.of(existingLike));  // Like already exists

        // When: User toggles like
        likeService.toggleLike(1L, 1L);

        // Then: The like should be deleted
        verify(likeRepository, times(0)).save(any(Like.class));
        verify(likeRepository, times(1)).delete(existingLike);
    }
}

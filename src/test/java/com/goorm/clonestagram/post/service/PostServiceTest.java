package com.goorm.clonestagram.post.service;

import com.goorm.clonestagram.post.ContentType;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.post.repository.PostsRepository;
import com.goorm.clonestagram.user.domain.Users;
import com.goorm.clonestagram.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PostServiceTest {

    @Mock
    private PostsRepository postsRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PostService postService;

    private Users testUser;
    private Posts testPost;

    @BeforeEach
    void setUp() {
        testUser = new Users();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testPost = new Posts();
        testPost.setId(1L);
        testPost.setContent("테스트 게시물");
        testPost.setUser(testUser);
        testPost.setContentType(ContentType.IMAGE);
        testPost.setMediaName("test-image.jpg");
        testPost.setCreatedAt(LocalDateTime.now());
        testPost.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void 게시물_저장_성공() {
        // given
        when(postsRepository.save(any(Posts.class))).thenReturn(testPost);

        // when
        Posts result = postService.save(testPost);

        // then
        assertNotNull(result);
        assertEquals(testPost.getContent(), result.getContent());
        assertEquals(testPost.getContentType(), result.getContentType());
        assertEquals(testPost.getMediaName(), result.getMediaName());
        verify(postsRepository).save(any(Posts.class));
    }

    @Test
    void 게시물_조회_성공() {
        // given
        when(postsRepository.findByIdAndDeletedIsFalse(anyLong())).thenReturn(Optional.of(testPost));

        // when
        Posts result = postService.findByIdAndDeletedIsFalse(1L);

        // then
        assertNotNull(result);
        assertEquals(testPost.getContent(), result.getContent());
        assertEquals(testPost.getContentType(), result.getContentType());
        assertEquals(testPost.getMediaName(), result.getMediaName());
    }

    @Test
    void 게시물_조회_실패_게시물없음() {
        // given
        when(postsRepository.findByIdAndDeletedIsFalse(anyLong())).thenReturn(Optional.empty());

        // when & then
        assertThrows(IllegalArgumentException.class, () -> postService.findByIdAndDeletedIsFalse(1L));
    }

    @Test
    void 게시물_존재여부_확인_성공() {
        // given
        when(postsRepository.existsByIdAndDeletedIsFalse(anyLong())).thenReturn(true);

        // when
        boolean result = postService.existsByIdAndDeletedIsFalse(1L);

        // then
        assertTrue(result);
        verify(postsRepository).existsByIdAndDeletedIsFalse(1L);
    }

    @Test
    void 게시물_존재여부_확인_실패() {
        // given
        when(postsRepository.existsByIdAndDeletedIsFalse(anyLong())).thenReturn(false);

        // when
        boolean result = postService.existsByIdAndDeletedIsFalse(1L);

        // then
        assertFalse(result);
        verify(postsRepository).existsByIdAndDeletedIsFalse(1L);
    }

    @Test
    void 사용자의_게시물_목록_조회_성공() {
        // given
        List<Posts> posts = Arrays.asList(testPost);
        when(postsRepository.findAllByUserIdAndDeletedIsFalse(anyLong())).thenReturn(posts);

        // when
        List<Posts> result = postService.findAllByUserIdAndDeletedIsFalse(1L);

        // then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(testPost.getContent(), result.get(0).getContent());
        verify(postsRepository).findAllByUserIdAndDeletedIsFalse(1L);
    }

    @Test
    void 사용자의_게시물_목록_조회_실패_게시물없음() {
        // given
        when(postsRepository.findAllByUserIdAndDeletedIsFalse(anyLong())).thenReturn(Arrays.asList());

        // when
        List<Posts> result = postService.findAllByUserIdAndDeletedIsFalse(1L);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(postsRepository).findAllByUserIdAndDeletedIsFalse(1L);
    }
} 
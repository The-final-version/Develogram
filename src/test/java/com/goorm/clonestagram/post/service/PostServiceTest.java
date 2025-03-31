package com.goorm.clonestagram.post.service;


import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.post.dto.PostResDto;
import com.goorm.clonestagram.post.repository.PostsRepository;
import com.goorm.clonestagram.post.service.PostService;
import com.goorm.clonestagram.user.domain.User;
import com.goorm.clonestagram.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PostServiceTest {

    @InjectMocks
    private PostService postService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostsRepository postsRepository;

    private Pageable pageable;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        pageable = PageRequest.of(0, 10);
    }

    /**
     * POST001 - 내 피드 조회 - 정상동작
     */
    @Test
    void 내_피드_조회_정상동작() {
        // given
        User user = User.builder().id(1L).username("testuser").build();
        Posts post = Posts.builder().id(1L).user(user).content("test").build();
        Page<Posts> page = new PageImpl<>(List.of(post));

        when(userRepository.findByIdAndDeletedIsFalse(1L)).thenReturn(Optional.of(user));
        when(postsRepository.findAllByUserIdAndDeletedIsFalse(1L, pageable)).thenReturn(page);

        // when
        PostResDto result = postService.getMyFeed(1L, pageable);

        // then
        assertEquals("testuser", result.getUser().getUsername());
        assertEquals(1, result.getFeed().getTotalElements());
    }

    /**
     * POST002 - 내 피드 조회 - 유저 없음
     */
    @Test
    void 내_피드_조회_유저없음() {
        // given
        when(userRepository.findByIdAndDeletedIsFalse(999L)).thenReturn(Optional.empty());

        // when & then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                postService.getMyFeed(999L, pageable));
        assertTrue(exception.getMessage().contains("userId = 999"));
    }

    /**
     * POST003 - 전체 피드 조회 - 정상동작
     */
    @Test
    void 전체_피드_조회() {
        // given
        Posts post = Posts.builder().id(1L).content("전체 피드").build();
        Page<Posts> page = new PageImpl<>(List.of(post));
        when(postsRepository.findAllByDeletedIsFalse(pageable)).thenReturn(page);

        // when
        PostResDto result = postService.getAllFeed(pageable);

        // then
        assertEquals(1, result.getFeed().getTotalElements());
        assertEquals("전체 피드", result.getFeed().getContent().get(0).getContent());
    }

    /**
     * POST004 - 팔로우 피드 조회 - 정상동작
     */
    @Test
    void 팔로우_피드_정상동작() {
        // given
        User user = User.builder().id(1L).username("me").build();
        List<Long> followingIds = List.of(2L, 3L);
        Posts post = Posts.builder().id(100L).user(user).content("팔로우 피드").build();
        Page<Posts> page = new PageImpl<>(List.of(post));

        when(userRepository.findByIdAndDeletedIsFalse(1L)).thenReturn(Optional.of(user));
        when(userRepository.findFollowingUserIdsByFromUserId(1L)).thenReturn(followingIds);
        when(postsRepository.findAllByUserIdInAndDeletedIsFalse(followingIds, pageable)).thenReturn(page);

        // when
        PostResDto result = postService.getFollowFeed(1L, pageable);

        // then
        assertEquals(1, result.getFeed().getTotalElements());
        assertEquals("팔로우 피드", result.getFeed().getContent().get(0).getContent());
    }

    /**
     * POST005 - 팔로우 피드 조회 - 유저 없음
     */
    @Test
    void 팔로우_피드_유저없음() {
        // given
        when(userRepository.findByIdAndDeletedIsFalse(999L)).thenReturn(Optional.empty());

        // when & then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> postService.getFollowFeed(999L, pageable));
        assertTrue(exception.getMessage().contains("userId = 999"));
    }
}


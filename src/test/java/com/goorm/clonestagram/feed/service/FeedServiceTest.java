package com.goorm.clonestagram.feed.service;

import com.goorm.clonestagram.feed.domain.Feeds;
import com.goorm.clonestagram.user.domain.Users;
import com.goorm.clonestagram.feed.dto.FeedResponseDto;
import com.goorm.clonestagram.feed.repository.FeedRepository;
import com.goorm.clonestagram.follow.repository.FollowRepository;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.post.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FeedServiceTest {

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Mock
    private FeedRepository feedRepository;

    @Mock
    private FollowRepository followRepository;

    @InjectMocks
    private FeedService feedService;

    @Test
    void F01_사용자_피드_조회_성공() {
        // given
        Long userId = 1L;
        int page = 0;
        int size = 10;

        Users user = new Users(userId);
        com.goorm.clonestagram.user.domain.Users postOwner = com.goorm.clonestagram.user.domain.Users.builder()
                .id(99L)
                .username("postOwner")
                .email("owner@example.com")
                .password("pw")
                .build();

        Posts post = Posts.builder()
                .id(101L)
                .content("테스트 내용")
                .contentType(ContentType.IMAGE)
                .user(postOwner) // ✅ 추가
                .build();

        Feeds feeds = Feeds.builder()
                .user(user)
                .post(post)
                .build();

        Page<Feeds> mockFeedPage = new PageImpl<>(List.of(feeds), PageRequest.of(page, size), 1);

        when(feedRepository.findByUserIdWithPostAndUser(eq(userId), any(Pageable.class)))
                .thenReturn(mockFeedPage);


        // when
        Page<FeedResponseDto> result = feedService.getUserFeed(userId, page, size);

        // then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("테스트 내용", result.getContent().get(0).getContent());

        verify(feedRepository).findByUserIdWithPostAndUser(eq(userId), any(Pageable.class));
    }

    @Test
    void F02_사용자_피드가_비어있을_때() {
        // given
        Long userId = 2L;
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);

        Page<Feeds> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(feedRepository.findByUserIdWithPostAndUser(eq(userId), any(Pageable.class)))
                .thenReturn(emptyPage);

        // when
        Page<FeedResponseDto> result = feedService.getUserFeed(userId, page, size);

        // then
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());

        verify(feedRepository).findByUserIdWithPostAndUser(eq(userId), any(Pageable.class));
    }

    @Test
    void F03_피드_생성_팔로워가_존재할_때() {
        // given
        Long postOwnerId = 1L;
        Long postId = 100L;

        // 가상의 팔로워 ID 목록
        List<Long> followerIds = List.of(10L, 20L, 30L);

        // 게시물
        Posts post = Posts.builder()
                .id(postId)
                .content("새 게시물")
                .contentType(ContentType.IMAGE)
                .user(com.goorm.clonestagram.user.domain.Users.builder().id(postOwnerId).build())
                .build();

        // followRepository 가 반환할 값
        when(followRepository.findFollowerIdsByFollowedId(postOwnerId)).thenReturn(followerIds);

        // when
        feedService.createFeedForFollowers(post);

        // then
        verify(followRepository).findFollowerIdsByFollowedId(postOwnerId);

        // Iterable<Feed>로 넘어오므로 size() 사용 불가 → count()로 체크
        verify(feedRepository).saveAll(argThat(feeds -> {
            long count = StreamSupport.stream(feeds.spliterator(), false).count();
            boolean allMatch = StreamSupport.stream(feeds.spliterator(), false)
                    .allMatch(feed -> followerIds.contains(feed.getUser().getId()) &&
                            feed.getPost().getId().equals(postId));
            return count == followerIds.size() && allMatch;
        }));
    }

    @Test
    void F04_피드_생성_팔로워가_없을_때() {
        // given
        Long postOwnerId = 2L;
        Long postId = 200L;

        // 팔로워가 없음
        List<Long> emptyFollowerList = List.of();

        Posts post = Posts.builder()
                .id(postId)
                .content("팔로워 없는 게시물")
                .contentType(ContentType.IMAGE)
                .user(com.goorm.clonestagram.user.domain.Users.builder().id(postOwnerId).build())
                .build();

        when(followRepository.findFollowerIdsByFollowedId(postOwnerId)).thenReturn(emptyFollowerList);

        // when
        feedService.createFeedForFollowers(post);

        // then
        verify(followRepository).findFollowerIdsByFollowedId(postOwnerId);
        verify(feedRepository, never()).saveAll(any());
    }

    @Test
    void F05_피드_삭제_게시물ID기반_성공() {
        // given
        Long postId = 123L;

        // when
        feedService.deleteFeedByPostId(postId);

        // then
        verify(feedRepository, times(1)).deleteByPostId(postId);
    }

    @Test
    void F06_사용자가_본_게시물_삭제() {
        // given
        Long userId = 1L;
        List<Long> postIds = List.of(101L, 102L, 103L);

        // when
        feedService.removeSeenFeeds(userId, postIds);

        // then
        verify(feedRepository, times(1))
                .deleteByUserIdAndPostIdIn(userId, postIds);
    }

    @Test
    void F07_삭제_요청에_빈_postIds_리스트_전달() {
        // given
        Long userId = 1L;
        List<Long> emptyPostIds = List.of();

        // when
        feedService.removeSeenFeeds(userId, emptyPostIds);

        // then
        verify(feedRepository, never())
                .deleteByUserIdAndPostIdIn(anyLong(), anyList());
    }

    @Test
    void F08_잘못된_userId로_feed_삭제_요청() {
        // given
        Long invalidUserId = -999L; // 존재하지 않는 유저 ID
        List<Long> postIds = List.of(101L, 102L);

        // when & then
        assertDoesNotThrow(() -> {
            feedService.removeSeenFeeds(invalidUserId, postIds);
        });

        verify(feedRepository).deleteByUserIdAndPostIdIn(invalidUserId, postIds);
    }

}

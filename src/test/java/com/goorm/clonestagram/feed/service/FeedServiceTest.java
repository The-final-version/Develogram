package com.goorm.clonestagram.feed.service;

import com.goorm.clonestagram.exception.FeedFetchFailedException;
import com.goorm.clonestagram.exception.UserNotFoundException;
import com.goorm.clonestagram.feed.domain.Feeds;
import com.goorm.clonestagram.follow.service.FollowService;
import com.goorm.clonestagram.user.domain.Users;
import com.goorm.clonestagram.user.service.UserService;
import com.goorm.clonestagram.feed.dto.FeedResponseDto;
import com.goorm.clonestagram.feed.repository.FeedRepository;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.util.MockEntityFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.data.domain.*;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class FeedServiceTest {
    @InjectMocks
    private FeedService feedService;

    @Mock
    private FeedRepository feedRepository;

    @Mock
    private UserService userService;

    @Mock
    private FollowService followService;

    private Users targetUser;   // 피드를 보는 유저
    private Users postOwner;

    @BeforeEach
    void setUp() {
        targetUser = MockEntityFactory.mockUser(1L, "targetUser");
        postOwner = MockEntityFactory.mockUser(99L, "postOwner");
    }

    @Test
    void F01_사용자_피드_조회_성공() {
        // given
        Long userId = 1L;
        int page = 0;
        int size = 10;

        Pageable pageable = PageRequest.of(page, size);
        Posts post = MockEntityFactory.mockPost(101L, "테스트 내용", postOwner);
        Feeds feed = MockEntityFactory.mockFeed(targetUser, post);
        Page<Feeds> feedPage = new PageImpl<>(List.of(feed), PageRequest.of(page, size), 1);

        when(userService.findByIdAndDeletedIsFalse(userId)).thenReturn(targetUser);
        when(feedRepository.findByUserIdWithPostAndUser(eq(userId), any(Pageable.class)))
                .thenReturn(feedPage);

        // when
        Page<FeedResponseDto> result = feedService.getUserFeed(userId, pageable);

        // then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("테스트 내용", result.getContent().get(0).getContent());

        verify(feedRepository, times(1)).findByUserIdWithPostAndUser(eq(targetUser.getId()), any(Pageable.class));
        verify(userService, times(1)).findByIdAndDeletedIsFalse(userId);
    }


    @Test
    void F02_사용자_피드가_비어있을_때() {
        // given
        Long userId = 2L;
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);

        Page<Feeds> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(userService.findByIdAndDeletedIsFalse(userId)).thenReturn(targetUser);
        when(feedRepository.findByUserIdWithPostAndUser(eq(userId), any(Pageable.class)))
                .thenReturn(emptyPage);

        // when
        Page<FeedResponseDto> result = feedService.getUserFeed(userId, pageable);

        // then
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());

        verify(userService, times(1)).findByIdAndDeletedIsFalse(userId);
        verify(feedRepository).findByUserIdWithPostAndUser(eq(userId), any(Pageable.class));
    }


    @Test
    void F03_존재하지_않는_사용자_예외() {
        // given
        Long userId = 999L;
        Pageable pageable = PageRequest.of(0, 10);
        when(userService.findByIdAndDeletedIsFalse(userId))
                .thenThrow(new UserNotFoundException(userId));

        // when & then
        assertThrows(UserNotFoundException.class, () -> {
            feedService.getUserFeed(userId, pageable);
        });

        assertThrows(UserNotFoundException.class, () -> {
            feedService.getFollowFeed(userId, pageable);
        });

        verify(userService, times(2)).findByIdAndDeletedIsFalse(userId); // getUserFeed + getFollowFeed
    }


    @Test
    void F05_피드_조회_실패_예외() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Long userId = 1L;
        Users user = MockEntityFactory.mockUser(userId, "user");
        when(userService.findByIdAndDeletedIsFalse(userId)).thenReturn(user);
        when(feedRepository.findByUserIdWithPostAndUser(eq(userId), any()))
                .thenThrow(new RecoverableDataAccessException("DB Error"));

        // when & then
        assertThrows(FeedFetchFailedException.class, () -> {
            feedService.getUserFeed(userId, pageable);
        });

        verify(userService, times(1)).findByIdAndDeletedIsFalse(userId);
        verify(feedRepository, times(1)).findByUserIdWithPostAndUser(eq(userId), any());
    }


    @Test
    void F07_전체_피드_조회_성공() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Users user = MockEntityFactory.mockUser(1L, "user");
        Posts post = MockEntityFactory.mockPost(1L, "내용", user);
        Feeds feed = MockEntityFactory.mockFeed(user, post);
        Page<Feeds> feeds = new PageImpl<>(List.of(feed));

        when(feedRepository.findAllByDeletedIsFalse(pageable)).thenReturn(feeds);

        // when
        Page<FeedResponseDto> result = feedService.getAllFeed(pageable);

        // then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("내용", result.getContent().get(0).getContent());
        verify(feedRepository, times(1)).findAllByDeletedIsFalse(pageable);
    }


    @Test
    void F08_전체_피드_조회_실패_예외() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        when(feedRepository.findAllByDeletedIsFalse(pageable))
                .thenThrow(new RecoverableDataAccessException("DB 오류"));

        // when & then
        assertThrows(FeedFetchFailedException.class, () -> {
            feedService.getAllFeed(pageable);
        });

        verify(feedRepository, times(1)).findAllByDeletedIsFalse(pageable);
    }


    @Test
    void F09_팔로우_피드_조회_성공() {
        // given
        Long userId = 1L;
        Users user = MockEntityFactory.mockUser(userId, "user");
        Pageable pageable = PageRequest.of(0, 10);

        List<Long> followingIds = List.of(2L, 3L);
        Posts post = MockEntityFactory.mockPost(100L, "hello", MockEntityFactory.mockUser(2L, "user2"));
        Feeds feed = MockEntityFactory.mockFeed(user, post);
        Page<Feeds> feedPage = new PageImpl<>(List.of(feed), pageable, 1);

        when(userService.findByIdAndDeletedIsFalse(userId)).thenReturn(user);
        when(followService.findFollowingUserIdsByFollowerId(userId)).thenReturn(followingIds);
        when(feedRepository.findAllByUserIdInAndDeletedIsFalse(followingIds, pageable)).thenReturn(feedPage);

        // when
        Page<FeedResponseDto> result = feedService.getFollowFeed(userId, pageable);

        // then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("hello", result.getContent().get(0).getContent());

        verify(userService).findByIdAndDeletedIsFalse(userId);
        verify(followService).findFollowingUserIdsByFollowerId(userId);
        verify(feedRepository).findAllByUserIdInAndDeletedIsFalse(followingIds, pageable);
    }


    @Test
    void F10_팔로우_피드_조회_실패_예외() {
        // given
        Long userId = 1L;
        Users user = MockEntityFactory.mockUser(userId, "user");
        Pageable pageable = PageRequest.of(0, 10);

        when(userService.findByIdAndDeletedIsFalse(userId)).thenReturn(user);
        when(followService.findFollowingUserIdsByFollowerId(userId))
                .thenThrow(new RuntimeException("DB 오류"));

        // when & then
        assertThrows(FeedFetchFailedException.class, () -> {
            feedService.getFollowFeed(userId, pageable);
        });

        verify(userService).findByIdAndDeletedIsFalse(userId);
        verify(followService).findFollowingUserIdsByFollowerId(userId);
    }


    @Test
    void F11_게시물_업로드_시_팔로워_피드_생성() {
        // given
        Users postOwner = MockEntityFactory.mockUser(1L, "postOwner");
        Posts post = MockEntityFactory.mockPost(100L, "새 게시물", postOwner);

        List<Long> followerIds = List.of(2L, 3L);

        when(followService.findFollowerIdsByFollowedId(postOwner.getId()))
                .thenReturn(followerIds);

        // when
        feedService.createFeedForFollowers(post);

        // then
        verify(followService).findFollowerIdsByFollowedId(postOwner.getId());
        verify(feedRepository).saveAll(argThat(feeds -> {
            List<Feeds> list = StreamSupport.stream(feeds.spliterator(), false)
                    .collect(Collectors.toList());

            return list.size() == 2 &&
                    list.get(0).getUser().getId().equals(2L) &&
                    list.get(1).getUser().getId().equals(3L);
        }));
    }


    @Test
    void F12_팔로워_없으면_피드_생성되지_않음() {
        // given
        Users postOwner = MockEntityFactory.mockUser(1L, "postOwner");
        Posts post = MockEntityFactory.mockPost(100L, "내용", postOwner);

        when(followService.findFollowerIdsByFollowedId(postOwner.getId()))
                .thenReturn(Collections.emptyList());

        // when
        feedService.createFeedForFollowers(post);

        // then
        verify(feedRepository, never()).saveAll(any());
    }


    @Test
    void F13_게시물_삭제_시_피드_삭제() {
        // given
        Long postId = 100L;

        // when
        feedService.deleteFeedsByPostId(postId);

        // then
        verify(feedRepository, times(1)).deleteByPostId(postId);
    }


    @Test
    void F14_postId_null_예외() {
        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            feedService.deleteFeedsByPostId(null);
        });

        verify(feedRepository, never()).deleteByPostId(any());
    }


    @Test
    void F15_삭제된_게시글_피드_조회_안됨() {
        // given
        Users user = MockEntityFactory.mockUser(1L, "user");
        Pageable pageable = PageRequest.of(0, 10);

        // 삭제된 게시글이기 때문에 실제로는 피드가 조회되지 않아야 함
        when(userService.findByIdAndDeletedIsFalse(user.getId())).thenReturn(user);
        when(feedRepository.findByUserIdWithPostAndUser(user.getId(), pageable))
                .thenReturn(Page.empty());

        // when
        Page<FeedResponseDto> result = feedService.getUserFeed(user.getId(), pageable);

        // then
        assertTrue(result.isEmpty()); // ✅ 성공: 조회된 피드가 없어야 함
    }


    @Test
    void F16_removeSeenFeeds_성공() {
        // given
        Long userId = 1L;
        List<Long> seenPostIds = List.of(100L, 101L, 102L);

        // when
        feedService.removeSeenFeeds(userId, seenPostIds);

        // then
        verify(feedRepository, times(1)).deleteByUserIdAndPostIdIn(userId, seenPostIds);
    }


    @Test
    void F17_removeSeenFeeds_빈_리스트() {
        // given
        Long userId = 1L;
        List<Long> postIds = Collections.emptyList();

        // when
        feedService.removeSeenFeeds(userId, postIds);

        // then
        verify(feedRepository, never()).deleteByUserIdAndPostIdIn(any(), any());
    }


    @Test
    void F18_removeSeenFeeds_userId_null() {
        // given
        List<Long> postIds = List.of(100L);

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            feedService.removeSeenFeeds(null, postIds);
        });

        verify(feedRepository, never()).deleteByUserIdAndPostIdIn(any(), any());
    }


    @Test
    void F19_deleteAllByUser_성공() {
        // given
        Long userId = 1L;
        Users user = MockEntityFactory.mockUser(userId, "user");
        Posts post1 = MockEntityFactory.mockPost(100L, "post1", user);
        Posts post2 = MockEntityFactory.mockPost(101L, "post2", user);

        List<Feeds> userFeeds = List.of(
                MockEntityFactory.mockFeed(user, post1),
                MockEntityFactory.mockFeed(user, post2)
        );

        when(feedRepository.findByUserId(userId)).thenReturn(userFeeds);

        // when
        feedService.deleteAllByUser(userId);

        // then
        verify(feedRepository).findByUserId(userId);
        verify(feedRepository).deleteAll(userFeeds);
    }


    @Test
    void F20_deleteAllByUser_빈피드() {
        // given
        Long userId = 1L;
        when(feedRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

        // when
        feedService.deleteAllByUser(userId);

        // then
        verify(feedRepository).findByUserId(userId);
        verify(feedRepository, never()).deleteAll(any());
    }


    @Test
    void F21_팔로우_피드_조회_리스트_null_or_empty_처리() {
        // given
        Long userId = 1L;
        Users user = MockEntityFactory.mockUser(userId, "user");
        Pageable pageable = PageRequest.of(0, 10);

        when(userService.findByIdAndDeletedIsFalse(userId)).thenReturn(user);
        when(followService.findFollowingUserIdsByFollowerId(userId)).thenReturn(null);

        // when
        Page<FeedResponseDto> result = feedService.getFollowFeed(userId, pageable);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(userService).findByIdAndDeletedIsFalse(userId);
        verify(followService).findFollowingUserIdsByFollowerId(userId);
        verify(feedRepository, never()).findAllByUserIdInAndDeletedIsFalse(any(), any());
    }

    @Test
    void F22_전체_피드_조회_중_예기치_못한_예외() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        when(feedRepository.findAllByDeletedIsFalse(pageable))
                .thenThrow(new RuntimeException("예상 못한 오류"));

        // when & then
        assertThrows(FeedFetchFailedException.class, () -> {
            feedService.getAllFeed(pageable);
        });

        verify(feedRepository).findAllByDeletedIsFalse(pageable);
    }

    @Test
    void F23_팔로우_피드_DB_예외_발생시_커버() {
        // given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Users user = MockEntityFactory.mockUser(userId, "user");

        when(userService.findByIdAndDeletedIsFalse(userId)).thenReturn(user);
        when(followService.findFollowingUserIdsByFollowerId(userId)).thenReturn(List.of(2L, 3L));
        when(feedRepository.findAllByUserIdInAndDeletedIsFalse(any(), any()))
                .thenThrow(new RecoverableDataAccessException("DB 오류"));

        // when & then
        assertThrows(FeedFetchFailedException.class, () -> {
            feedService.getFollowFeed(userId, pageable);
        });

        verify(feedRepository).findAllByUserIdInAndDeletedIsFalse(any(), any());
    }

    @Test
    void F24_removeSeenFeeds_userId_null_예외_발생() {
        // given
        List<Long> postIds = List.of(100L);

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            feedService.removeSeenFeeds(null, postIds);
        });

    }

    @Test
    void F25_deleteAllByUser_userId_null_예외_발생() {
        // given
        List<Long> postIds = List.of(100L);

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            feedService.deleteAllByUser(null);
        });
    }

    @Test
    void F23_팔로우목록_없을때_빈피드_반환() {
        // given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Users user = MockEntityFactory.mockUser(userId, "user");

        when(userService.findByIdAndDeletedIsFalse(userId)).thenReturn(user);
        when(followService.findFollowingUserIdsByFollowerId(userId)).thenReturn(Collections.emptyList());

        // when
        Page<FeedResponseDto> result = feedService.getFollowFeed(userId, pageable);

        // then
        assertNotNull(result);
        assertEquals(0, result.getTotalElements()); // ✅ 빈 피드 반환 확인
        verify(feedRepository, never()).findAllByUserIdInAndDeletedIsFalse(any(), any());
    }

    @Test
    void F24_removeSeenFeeds_postIds_null() {
        // given
        Long userId = 1L;

        // when
        feedService.removeSeenFeeds(userId, null);

        // then
        verify(feedRepository, never()).deleteByUserIdAndPostIdIn(any(), any());
    }

    @Test
    void F25_removeSeenFeeds_postIds_empty() {
        // given
        Long userId = 1L;
        List<Long> postIds = Collections.emptyList();

        // when
        feedService.removeSeenFeeds(userId, postIds);

        // then
        verify(feedRepository, never()).deleteByUserIdAndPostIdIn(any(), any());
    }

}

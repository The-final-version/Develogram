package com.goorm.clonestagram.feed.service;

import com.goorm.clonestagram.exception.FeedFetchFailedException;
import com.goorm.clonestagram.exception.UserNotFoundException;
import com.goorm.clonestagram.feed.domain.Feeds;
import com.goorm.clonestagram.feed.dto.FeedResponseDto;
import com.goorm.clonestagram.feed.repository.FeedRepository;
import com.goorm.clonestagram.follow.service.FollowService;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.follow.repository.FollowRepository;
import com.goorm.clonestagram.post.dto.PostInfoDto;
import com.goorm.clonestagram.post.dto.PostResDto;
import com.goorm.clonestagram.post.service.PostService;
import com.goorm.clonestagram.user.domain.Users;
import com.goorm.clonestagram.user.dto.UserProfileDto;
import com.goorm.clonestagram.user.repository.UserRepository;
import com.goorm.clonestagram.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedService {

    private final FeedRepository feedRepository;
    private final FollowService followService;
    private final UserService userService;
    private final FollowRepository followRepository;

    @Transactional(readOnly = true)
    public Page<FeedResponseDto> getUserFeed(Long userId, Pageable pageable) {
        Users user = userService.findByIdAndDeletedIsFalse(userId);

        try {
            Page<Feeds> feeds = feedRepository.findByUserIdWithPostAndUser(userId, pageable);
            return feeds.map(FeedResponseDto::from);
        } catch (DataAccessException e) {
            throw new FeedFetchFailedException("피드 조회 중 DB 오류가 발생했습니다.");
        }
    }


    @Transactional(readOnly = true)
    public Page<FeedResponseDto> getAllFeed(Pageable pageable) {

        try {
            Page<Feeds> allFeed = feedRepository.findAllByDeletedIsFalse(pageable);
            return allFeed.map(FeedResponseDto::from);
        } catch (DataAccessException e) {
            throw new FeedFetchFailedException("피드 전체 조회 중 DB 오류가 발생했습니다.");
        } catch (Exception e) {
            throw new FeedFetchFailedException("피드 전체 조회 중 예기치 못한 오류가 발생했습니다.");
        }
    }


    @Transactional(readOnly = true)
    public Page<FeedResponseDto> getFollowFeed(Long userId, Pageable pageable) {
        Users user = userService.findByIdAndDeletedIsFalse(userId);

        try {
            List<Long> followList = followService.findFollowingUserIdsByFollowerId(user.getId());

            if (followList == null || followList.isEmpty()) {
                Page<Feeds> emptyPage = new PageImpl<>(List.of(), pageable, 0);
                return emptyPage.map(FeedResponseDto::from);
            }

            Page<Feeds> followingFeed = feedRepository.findAllByUserIdInAndDeletedIsFalse(followList, pageable);
            return followingFeed.map(FeedResponseDto::from);

        } catch (DataAccessException e) {
            throw new FeedFetchFailedException("팔로우 피드 조회 중 DB 오류가 발생했습니다.");
        } catch (Exception e) {
            throw new FeedFetchFailedException("팔로우 피드 조회 중 예기치 못한 오류가 발생했습니다.");
        }
    }



    @Transactional
    public void removeSeenFeeds(Long userId, List<Long> postIds) {
        if (userId == null) {
            throw new IllegalArgumentException("userId는 null일 수 없습니다.");
        }

        if (postIds == null || postIds.isEmpty()) {
            return; // 아무 것도 삭제하지 않음
        }

        feedRepository.deleteByUserIdAndPostIdIn(userId, postIds);
    }


    @Transactional
    public void deleteAllByUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId는 null일 수 없습니다.");
        }

        List<Feeds> userFeeds = feedRepository.findByUserId(userId);
        if (!userFeeds.isEmpty()) {
            feedRepository.deleteAll(userFeeds);
        }
    }



    //A의 게시물이 업로드될 때: A 팔로워들의 피드 생성
    @Transactional
    public void createFeedForFollowers(Posts post) {
        Long postOwnerId = post.getUser().getId();
        List<Long> followerIds = followService.findFollowerIdsByFollowedId(postOwnerId);

        if (followerIds.isEmpty()) {
            return;
        }

        List<Feeds> feeds = convertToFeeds(followerIds, post.getId());
        feedRepository.saveAll(feeds);
    }



    private List<Feeds> convertToFeeds(List<Long> followerIds, Long postId) {
        return followerIds.stream()
                .map(followerId -> new Feeds(followerId, postId))
                .toList();
    }


    // 게시물이 삭제될 때: 해당 게시물에 대한 피드 전부 삭제
    @Transactional
    public void deleteFeedsByPostId(Long postId) {
        if (postId == null) {
            throw new IllegalArgumentException("postId는 null일 수 없습니다.");
        }
        feedRepository.deleteByPostId(postId);
    }

}

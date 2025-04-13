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
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedService {

    private static final Logger auditLog = LoggerFactory.getLogger("com.goorm.clonestagram.audit");

    private final FeedRepository feedRepository;
    private final FollowService followService;
    private final UserService userService;
    private final FollowRepository followRepository;

    @Transactional(readOnly = true)
    public Page<FeedResponseDto> getUserFeed(Long userId, Pageable pageable) {
        Users user = userService.findByIdAndDeletedIsFalse(userId);

        try {
            Page<Feeds> feeds = feedRepository.findByUserIdWithPostAndUser(userId, pageable);
            // ✅ 강제 평가
            feeds.map(FeedResponseDto::from).getContent(); // 이게 있어야 예외 발생됨

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
            log.info("🚀 getFollowFeed 진입 - userId: {}", userId);
            List<Long> followList = followService.findFollowingUserIdsByFollowerId(user.getId());

            if (followList == null || followList.isEmpty()) {
                Page<Feeds> emptyPage = new PageImpl<>(List.of(), pageable, 0);
                return emptyPage.map(FeedResponseDto::from);
            }

            log.info("🔁 feedRepository.findAllByUserIdInWithPostAndUser 실행");

            Page<Feeds> followingFeed = feedRepository.findAllByUserIdAndPostOwnerInWithPostAndUser(userId, followList, pageable);

            log.info("📦 followingFeed size = {}", followingFeed.getContent().size());
            followingFeed.forEach(f -> log.info("🎯 post.media = {}", f.getPost().getMediaName()));

            return followingFeed.map(FeedResponseDto::from);


        } catch (DataAccessException e) {
            log.error("❌ DataAccessException 발생: {}", e.getMessage(), e);
            throw new FeedFetchFailedException("팔로우 피드 조회 중 DB 오류가 발생했습니다.");
        } catch (Exception e) {
            log.error("❌ 일반 Exception 발생: {}", e.getMessage(), e);
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
    @Async("feedTaskExecutor")
    public void createFeedForFollowers(Posts post) {
        long start = System.currentTimeMillis();
        List<Long> followerIds = followService.findFollowerIdsByFollowedId(post.getUser().getId());

        auditLog.info("🔵 [START] 피드 생성 시작 - postId={}, followerCount={}", post.getId(), followerIds.size());

        if (followerIds.isEmpty()) {
            auditLog.info("⚠️ [SKIP] 팔로워 없음 - postId={}", post.getId());
            return;
        }

        int batchSize = 2000;
        List<List<Long>> partitions = partitionList(followerIds, batchSize);

        List<CompletableFuture<Void>> tasks = partitions.stream()
                .map(batch -> CompletableFuture.runAsync(() -> {
                    long batchStart = System.currentTimeMillis();

                    List<Feeds> feeds = batch.stream()
                            .map(followerId -> Feeds.builder()
                                    .user(Users.builder().id(followerId).build())
                                    .post(post)
                                    .build())
                            .toList();

                    feedRepository.saveAll(feeds);

                    long batchEnd = System.currentTimeMillis();
                    log.info("🟢 [BATCH DONE] inserted={}, duration={}ms, postId={}",
                            feeds.size(), (batchEnd - batchStart), post.getId());
                }))
                .toList();

        // 모든 작업 완료 대기
        tasks.forEach(CompletableFuture::join);

        long end = System.currentTimeMillis();
        auditLog.info("✅ [ALL DONE] 피드 생성 완료 - totalFollower={}, totalDuration={}ms, postId={}",
                followerIds.size(), (end - start), post.getId());
    }






//    private List<Feeds> convertToFeeds(List<Long> followerIds, Long postId) {
//        return followerIds.stream()
//                .map(followerId -> new Feeds(followerId, postId))
//                .toList();
//    }


    // 게시물이 삭제될 때: 해당 게시물에 대한 피드 전부 삭제
    @Transactional
    public void deleteFeedsByPostId(Long postId) {
        if (postId == null) {
            throw new IllegalArgumentException("postId는 null일 수 없습니다.");
        }
        feedRepository.deleteByPostId(postId);
    }

    public <T> List<List<T>> partitionList(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }

}

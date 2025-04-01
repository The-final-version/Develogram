package com.goorm.clonestagram.feed.service;

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
import com.goorm.clonestagram.user.service.UserService;
import lombok.RequiredArgsConstructor;
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
    private final PostService postService;
    private final FollowRepository followRepository;


    @Transactional
    public Page<FeedResponseDto> getUserFeed(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Feeds> feeds = feedRepository.findByUserIdWithPostAndUser(userId, pageable);

        return feeds.map(FeedResponseDto::from);
    }



    public PostResDto getAllFeed(Pageable pageable) {
        //1. DB에 저장된 모든 피드 조회
        Page<Posts> myFeed = feedRepository.findAllByDeletedIsFalse(pageable);

        //2. 모든 작업이 완료된 경우 반환
        return PostResDto.builder()
                .feed(myFeed.map(PostInfoDto::fromEntity))
                .build();
    }


    public PostResDto getFollowFeed(Long userId, Pageable pageable) {
        // 1. 유저 조회
        Users users = userService.findByIdAndDeletedIsFalse(userId);

        // 2. 팔로잉 ID 목록 조회
        List<Long> followList = followService.findFollowingUserIdsByFollowerId(users.getId());

        // 3. 팔로잉 유저가 없으면 빈 페이지 리턴
        if (followList == null || followList.isEmpty()) {
            return PostResDto.builder()
                    .user(UserProfileDto.fromEntity(users))
                    .feed(Page.empty(pageable)) // ✅ 빈 페이지로 리턴
                    .build();
        }

        // 4. 해당 유저들의 게시글 조회
        Page<Posts> postsLists = feedRepository.findAllByUserIdInAndDeletedIsFalse(followList, pageable);

        // 5. 리턴
        return PostResDto.builder()
                .user(UserProfileDto.fromEntity(users))
                .feed(postsLists.map(PostInfoDto::fromEntity))
                .build();
    }


    @Transactional
    public void removeSeenFeeds(Long userId, List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) return;
        feedRepository.deleteByUserIdAndPostIdIn(userId, postIds);
    }


    @Transactional
    public void deleteAllByUser(Long userId) {
        List<Feeds> userFeeds = feedRepository.findByUserId(userId);
        feedRepository.deleteAll(userFeeds);
    }


    //A의 게시물이 업로드될 때: A 팔로워들의 피드 생성
    public void createFeedForFollowers(Posts post) {
        Long userId = post.getUser().getId();
        List<Long> followerIds = followRepository.findFollowerIdsByFollowedId(userId);

        if (followerIds == null || followerIds.isEmpty()) {
            return;
        }

        List<Feeds> feeds = followerIds.stream()
                .map(followerId -> new Feeds(followerId, post.getId()))
                .toList();

        feedRepository.saveAll(feeds);
    }

    // 게시물이 삭제될 때: 해당 게시물에 대한 피드 전부 삭제
    public void deleteFeedByPostId(Long postId) {
        feedRepository.deleteByPostId(postId);
    }
}

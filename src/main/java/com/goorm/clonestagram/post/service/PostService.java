package com.goorm.clonestagram.post.service;

import com.goorm.clonestagram.like.repository.LikeRepository;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.post.dto.PostResDto;
import com.goorm.clonestagram.post.dto.PostInfoDto;
import com.goorm.clonestagram.post.repository.PostsRepository;
import com.goorm.clonestagram.user.domain.User;
import com.goorm.clonestagram.user.dto.UserProfileDto;
import com.goorm.clonestagram.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 피드 조회 요청을 처리하는 서비스
 * - 유저의 정보를 받아 피드 조회 서비스 수행
 */
@Service
@Transactional
@RequiredArgsConstructor
public class PostService {

    private final UserRepository userRepository;
    private final PostsRepository postsRepository;
    private final LikeRepository likeRepository;
    /**
     * 본인 게시물 조회
     * 유저 id를 활용해 유저 정보 조회
     * 해당 유저가 작성한 모든 피드 반환
     *
     * @param userId 조회를 요청한 유저
     * @param pageable 페이징 처리
     * @return 유저 정보, 해당 유저가 작성한 피드 리스트
     */
    public PostResDto getMyFeed(Long userId, Pageable pageable) {
        //1. userId를 활용해 유저 객체 조회
        User user = userRepository.findByIdAndDeletedIsFalse(userId)
                .orElseThrow(() -> new IllegalArgumentException("userId = " + userId + " 인 유저가 존재하지 않습니다"));

        //2. 해당 유저가 작성한 모든 피드 조회, 페이징 처리
        Page<Posts> myFeed = postsRepository.findAllByUserIdAndDeletedIsFalse(user.getId(), pageable);

        //3. 모든 작업이 완료도니 경우 응답 반환
        return PostResDto.builder()
                .user(UserProfileDto.fromEntity(user))
                .feed(myFeed.map(PostInfoDto::fromEntity))
                .build();
    }

    /**
     * 모든 피드 조회
     * 모든 유저가 작성한 피드 반환
     *
     * @param pageable 페이징 처리
     * @return 모든 유저가 작성한 피드 리스트
     */
    public PostResDto getAllFeed(Pageable pageable) {
        //1. DB에 저장된 모든 피드 조회
        Page<Posts> myFeed = postsRepository.findAllByDeletedIsFalse(pageable);

        //2. 모든 작업이 완료된 경우 반환
        return PostResDto.builder()
                .feed(myFeed.map(PostInfoDto::fromEntity))
                .build();
    }

    /**
     * 본인 피드 조회
     * 유저 id를 활용해 유저 정보 조회
     * 해당 유저가 작성한 모든 피드 반환
     *
     * @param userId 조회를 요청한 유저
     * @param pageable 페이징 처리
     * @return 유저 정보, 해당 유저가 작성한 피드 리스트
     */
    public PostResDto getFollowFeed(Long userId, Pageable pageable) {
        //1. userId를 활용해 유저 객체 조회
        User user = userRepository.findByIdAndDeletedIsFalse(userId)
                .orElseThrow(() -> new IllegalArgumentException("userId = " + userId + " 인 유저가 존재하지 않습니다"));

        //2. 해당 유저의 팔로우 리스트를 조회
        List<Long> followList = userRepository.findFollowingUserIdsByFromUserId(user.getId());
        //3. 팔로우 리스트에 있는 유저들의 피드들을 조회
        Page<Posts> postsLists = postsRepository.findAllByUserIdInAndDeletedIsFalse(followList, pageable);

        //4. 모든 작업이 완료된 경우 반환
        return PostResDto.builder()
                .user(UserProfileDto.fromEntity(user))
                .feed(postsLists.map(PostInfoDto::fromEntity))
                .build();
    }

    public boolean isPostLikedByLoginUser(Long postId, Long userId) {
        return likeRepository.existsByUserIdAndPostsId(userId, postId);
    }
}

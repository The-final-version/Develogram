package com.goorm.clonestagram.follow.service;

import com.goorm.clonestagram.follow.domain.Follows;
import com.goorm.clonestagram.follow.dto.FollowDto;
import com.goorm.clonestagram.follow.mapper.FollowMapper;
import com.goorm.clonestagram.follow.repository.FollowRepository;
import com.goorm.clonestagram.user.domain.Users;
import com.goorm.clonestagram.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final UserService userService;

    @Transactional
    public void toggleFollow(Long followerId, Long followedId) {
        if (followerId.equals(followedId)) {
            throw new IllegalArgumentException("자기 자신을 팔로우할 수 없습니다.");
        }

        Users follower = userService.findByIdAndDeletedIsFalse(followerId);
        Users followed = userService.findByIdAndDeletedIsFalse(followedId);

        // 락 걸고 조회
        Optional<Follows> followOpt = followRepository.findByFollowerAndFollowedWithLock(follower, followed);

        if (followOpt.isPresent()) {
            followRepository.delete(followOpt.get());
        } else {
            try {
                followRepository.save(new Follows(follower, followed));
            } catch (DataIntegrityViolationException e) {
                // 동시 요청으로 인해 중복 삽입 예외 발생 시 무시하거나 로그 처리
                log.warn("중복 팔로우 요청 감지: followerId={}, followedId={}", followerId, followedId);
            }
        }
    }



    @Transactional(readOnly = true)
    public List<FollowDto> getFollowingList(Long userId) {
        Users user = userService.findByIdAndDeletedIsFalse(userId);
        return followRepository.findFollowedAllByFollower(user).stream()
                .map(FollowMapper::toFollowingDto)
                .collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    public List<FollowDto> getFollowerList(Long userId) {
        Users user = userService.findByIdAndDeletedIsFalse(userId);
        return followRepository.findFollowerAllByFollowed(user).stream()
                .map(FollowMapper::toFollowerDto)
                .collect(Collectors.toList());
    }


    public List<Long> findFollowingUserIdsByFollowerId(Long userId){
        return followRepository.findFollowedIdsByFollowerId(userId);
    }


    public List<Long> findFollowerIdsByFollowedId(Long followedUserId) {
        return followRepository.findFollowerIdsByFollowedId(followedUserId);
    }

}

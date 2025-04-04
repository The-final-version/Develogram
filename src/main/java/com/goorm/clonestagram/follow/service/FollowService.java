package com.goorm.clonestagram.follow.service;

import com.goorm.clonestagram.follow.domain.Follows;
import com.goorm.clonestagram.follow.dto.FollowDto;
import com.goorm.clonestagram.follow.mapper.FollowMapper;
import com.goorm.clonestagram.follow.repository.FollowRepository;
import com.goorm.clonestagram.user.domain.Users;
import com.goorm.clonestagram.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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

        followRepository.findByFollowerAndFollowed(follower, followed)
                .ifPresentOrElse(follows -> {
                    followRepository.delete(follows);
                }, () -> {
                    Follows follows = new Follows(follower, followed);
                    followRepository.save(follows);
                });
    }


    @Transactional(readOnly = true)
    public List<FollowDto> getFollowingList(Long userId) {
        Users user = userService.findByIdAndDeletedIsFalse(userId);
        List<Follows> followsList = followRepository.findFollowingsByFollower(user); // ✅ 수정

        if (followsList == null || followsList.isEmpty()) {
            return Collections.emptyList();
        }

        return followsList.stream()
                .map(FollowMapper::toFollowingDto) // ✅ 수정
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FollowDto> getFollowerList(Long userId) {
        Users user = userService.findByIdAndDeletedIsFalse(userId);
        List<Follows> followsList = followRepository.findFollowersByFollowed(user);

        if (followsList == null || followsList.isEmpty()) {
            return Collections.emptyList();
        }

        return followsList.stream()
                .map(FollowMapper::toFollowerDto) // ✅ 수정
                .collect(Collectors.toList());
    }



    public List<Long> findFollowingUserIdsByFollowerId(Long userId){
        return followRepository.findFollowingUserIdsByFollowerId(userId);
    }

    public List<Long> findFollowerIdsByFollowedId(Long followedUserId) {
        return followRepository.findFollowerIdsByFollowedId(followedUserId);
    }

}

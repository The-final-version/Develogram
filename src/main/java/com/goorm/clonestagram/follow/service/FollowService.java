package com.goorm.clonestagram.follow.service;

import com.goorm.clonestagram.follow.domain.Follows;
import com.goorm.clonestagram.follow.dto.FollowDto;
import com.goorm.clonestagram.follow.mapper.FollowMapper;
import com.goorm.clonestagram.follow.repository.FollowRepository;
import com.goorm.clonestagram.user.domain.Users;
import com.goorm.clonestagram.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DeadlockLoserDataAccessException;
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
    private final FollowWriteService followWriteService;

    @Transactional
    public void toggleFollow(Long followerId, Long followedId) {
        int maxRetries = 3;
        int attempt = 0;

        while (attempt < maxRetries) {
            try {
                performToggle(followerId, followedId);
                return;
            } catch (DeadlockLoserDataAccessException | CannotAcquireLockException e) {
                attempt++;
                log.warn("\uD83D\uDD01 데드락 발생, 재시도 {}/{}: followerId={}, followedId={}, message={}",
                        attempt, maxRetries, followerId, followedId, e.getMessage());

                if (attempt >= maxRetries) {
                    throw e;
                }

                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {}
            }
        }
    }

    private void performToggle(Long followerId, Long followedId) {
        if (followerId.equals(followedId)) {
            throw new IllegalArgumentException("자기 자신을 팔로우할 수 없습니다.");
        }

        Users follower = userService.findByIdAndDeletedIsFalse(followerId);
        Users followed = userService.findByIdAndDeletedIsFalse(followedId);

        // 항상 followerId < followedId 순서 고정 → 데드락 방지
        Users first = follower.getId() < followed.getId() ? follower : followed;
        Users second = follower.getId() < followed.getId() ? followed : follower;

        Optional<Follows> followOpt = followRepository.findByFollowerAndFollowedWithLock(first.getId(), second.getId());

        if (followOpt.isPresent()) {
            followRepository.delete(followOpt.get());
        } else {
            Follows follows = new Follows(first, second);
            followWriteService.tryCreate(follows);
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

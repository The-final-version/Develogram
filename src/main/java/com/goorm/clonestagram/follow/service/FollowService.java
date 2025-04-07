package com.goorm.clonestagram.follow.service;

import com.goorm.clonestagram.follow.domain.Follows;
import com.goorm.clonestagram.follow.dto.FollowDto;
import com.goorm.clonestagram.follow.mapper.FollowMapper;
import com.goorm.clonestagram.follow.repository.FollowRepository;
import com.goorm.clonestagram.user.domain.service.UserExternalQueryService;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FollowService {

	private final FollowRepository followRepository;
	private final UserExternalQueryService userService;

	@Transactional
	public void toggleFollow(Long followerId, Long followedId) {
		if (followerId.equals(followedId)) {
			throw new IllegalArgumentException("자기 자신을 팔로우할 수 없습니다.");
		}

		UserEntity follower = new UserEntity(userService.findByIdAndDeletedIsFalse(followerId));
		UserEntity followed = new UserEntity(userService.findByIdAndDeletedIsFalse(followedId));

		followRepository.findByFollowerAndFollowed(follower, followed)
			.ifPresentOrElse(
				followRepository::delete,
				() -> followRepository.save(new Follows(follower, followed))
			);
	}

	@Transactional(readOnly = true)
	public List<FollowDto> getFollowingList(Long userId) {
		UserEntity user = new UserEntity(userService.findByIdAndDeletedIsFalse(userId));
		return followRepository.findFollowedAllByFollower(user).stream()
			.map(FollowMapper::toFollowingDto)
			.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<FollowDto> getFollowerList(Long userId) {
		UserEntity user = new UserEntity(userService.findByIdAndDeletedIsFalse(userId));
		return followRepository.findFollowerAllByFollowed(user).stream()
			.map(FollowMapper::toFollowerDto)
			.collect(Collectors.toList());
	}

	public List<Long> findFollowingUserIdsByFollowerId(Long userId) {
		return followRepository.findFollowedIdsByFollowerId(userId);
	}

	public List<Long> findFollowerIdsByFollowedId(Long followedUserId) {
		return followRepository.findFollowerIdsByFollowedId(followedUserId);
	}

}

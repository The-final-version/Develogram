package com.goorm.clonestagram.follow.controller;

import com.goorm.clonestagram.follow.dto.FollowDto;
import com.goorm.clonestagram.follow.service.FollowService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/follow")
public class FollowController {

	private final FollowService followService;

	@PostMapping(
		value = "/{follower}/profile/{followed}",
		produces = MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8"
	)
	public ResponseEntity<String> toggleFollow(
		@PathVariable("follower") Long follower,
		@PathVariable("followed") Long followed
	) {
		followService.toggleFollow(follower, followed);
		return ResponseEntity.ok("팔로우 상태가 변경되었습니다.");
	}

	@GetMapping(
		value = "/{userId}/profile/followers",
		produces = MediaType.APPLICATION_JSON_VALUE
	)
	public ResponseEntity<List<FollowDto>> getFollowers(@PathVariable("userId") Long userId) {
		List<FollowDto> followers = followService.getFollowerList(userId);
		return ResponseEntity.ok(followers);
	}

	@GetMapping(
		value = "/{userId}/profile/following",
		produces = MediaType.APPLICATION_JSON_VALUE
	)
	public ResponseEntity<List<FollowDto>> getFollowing(@PathVariable("userId") Long userId) {
		List<FollowDto> following = followService.getFollowingList(userId);
		return ResponseEntity.ok(following);
	}

}

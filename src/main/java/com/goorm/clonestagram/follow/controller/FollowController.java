package com.goorm.clonestagram.follow.controller;

import com.goorm.clonestagram.follow.dto.FollowDto;
import com.goorm.clonestagram.follow.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequiredArgsConstructor
@RequestMapping("/follow")
public class FollowController {

    private final FollowService followService;

    @PostMapping("/{follower}/profile/{followed}")
    public ResponseEntity<String> toggleFollow(@PathVariable Long follower, @PathVariable Long followed) {
        // 팔로우 상태를 확인하고 토글 처리
        followService.toggleFollow(follower, followed);
        return ResponseEntity.ok("팔로우 상태가 변경되었습니다.");
    }

    @GetMapping("/{userId}/profile/followers")
    public ResponseEntity<List<FollowDto>> getFollowers(@PathVariable Long userId) {
        List<FollowDto> followers = followService.getFollowerList(userId);
        return ResponseEntity.ok(followers);
    }


    @GetMapping("/{userId}/profile/following")
    public ResponseEntity<List<FollowDto>> getFollowing(@PathVariable Long userId) {
        List<FollowDto> following = followService.getFollowingList(userId);
        return ResponseEntity.ok(following);
    }

}

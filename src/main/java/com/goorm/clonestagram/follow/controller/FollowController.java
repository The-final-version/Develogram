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

    /**
     * 팔로워 목록 조회
     * @param userId 팔로워 목록을 조회할 사용자 ID
     * @return 팔로워 목록
     */
    @GetMapping("/{userId}/profile/followers")
    public ResponseEntity<List<FollowDto>> getFollowers(@PathVariable Long userId) {
        List<FollowDto> followers = followService.getFollowerList(userId);
        return ResponseEntity.ok(followers);
    }

    /**
     * 팔로잉 목록 조회
     * @param userId 팔로잉 목록을 조회할 사용자 ID
     * @return 팔로잉 목록
     */
    @GetMapping("/{userId}/profile/following")
    public ResponseEntity<List<FollowDto>> getFollowing(@PathVariable Long userId) {
        List<FollowDto> following = followService.getFollowingList(userId);
        return ResponseEntity.ok(following);
    }

    /**
     * 팔로우 상태 토글 (팔로우 / 언팔로우)
     * @param follower 팔로우를 요청하는 사용자 ID
     * @param followed 팔로우 대상 사용자 ID
     * @return 팔로우 상태 변경 성공 메시지
     */
    @PostMapping("/{follower}/profile/{followed}")
    public ResponseEntity<String> toggleFollow(@PathVariable Long follower, @PathVariable Long followed) {
        // 팔로우 상태를 확인하고 토글 처리
        followService.toggleFollow(follower, followed);
        return ResponseEntity.ok("팔로우 상태가 변경되었습니다.");
    }
}

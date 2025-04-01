package com.goorm.clonestagram.post.controller;

import com.goorm.clonestagram.post.dto.PostResDto;
import com.goorm.clonestagram.post.service.PostService;
import com.goorm.clonestagram.like.service.LikeService;
import com.goorm.clonestagram.util.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 피드 조회 요청을 처리하는 컨트롤러
 * - 클라이언트로부터 피드 조회 요청을 받아 검증 및 서비스 호출을 수행
 */
@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService feedService;
    private final LikeService likeService;
    private final PostService postService;
    /**
     * 본인 피드 조회
     * - 로그인 한 유저의 Id를 조회한 후 서비스 계층에 전달
     *
     * @param userDetail 로그인 한 유저에 대한 정보
     * @param pageable 페이징 처리
     * @return 조회 성공시 서비스 호출한 유저 정보와 피드 리스트 반환
     */
    @GetMapping("/feeds/user")
    public ResponseEntity<PostResDto> userFeed(@RequestParam("userId") Long userId,
                                               @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(feedService.getMyFeed(userId, pageable));
    }
    /**
     * 모든 피드 조회
     * - 모든 피드 조회 요청을 서비스 계층에 전달
     *
     * @param pageable 페이징 처리
     * @return 조회 성공시 서비스 호출한 유저 정보와 피드 리스트 반환
     */
    @GetMapping("/feeds/all")
    public ResponseEntity<PostResDto> allFeed(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)Pageable pageable
    ){

        return ResponseEntity.ok(feedService.getAllFeed(pageable));
    }

    /**
     * 팔로우 피드 조회
     * - 로그인 한 유저의 Id를 조회한 후 서비스 계층에 전달
     *
     * @param userDetail 로그인 한 유저에 대한 정보
     * @param pageable 페이징 처리
     * @return 조회 성공시 서비스 호출한 유저 정보와 피드 리스트 반환
     */
    @GetMapping("/feeds/follow")
    public ResponseEntity<PostResDto> followFeed(@AuthenticationPrincipal CustomUserDetails userDetail,
                                                 @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)Pageable pageable
    ){
        Long userId = userDetail.getId();

        return ResponseEntity.ok(feedService.getFollowFeed(userId, pageable));
    }

    // 특정 게시글의 좋아요 개수 조회
    @GetMapping("/feeds/{postId}/likes")
    public ResponseEntity<Long> getLikeCount(@PathVariable Long postId) {
        Long likeCount = likeService.getLikeCount(postId);
        return ResponseEntity.ok(likeCount);
    }
    // 특정 게시글에 좋아요 추가 또는 취소 (토글)
    @PostMapping("/feeds/{postId}/likes")
    public ResponseEntity<Void> toggleLike(@PathVariable Long postId, @AuthenticationPrincipal CustomUserDetails userDetail) {
        Long userId = userDetail.getId();
        likeService.toggleLike(userId, postId);  // 서비스 레이어에서 토글 처리
        return ResponseEntity.ok().build();
    }

    @GetMapping("/posts/{postId}/liked")
    public ResponseEntity<Boolean> checkIfLiked(@PathVariable Long postId,
                                                @AuthenticationPrincipal CustomUserDetails userDetails) {
        boolean liked = postService.isPostLikedByLoginUser(postId, userDetails.getId());
        return ResponseEntity.ok(liked);
    }
}

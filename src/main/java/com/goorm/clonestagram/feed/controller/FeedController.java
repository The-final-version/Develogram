package com.goorm.clonestagram.feed.controller;

import com.goorm.clonestagram.feed.dto.FeedResponseDto;
import com.goorm.clonestagram.feed.dto.SeenRequest;
import com.goorm.clonestagram.feed.service.FeedService;
import com.goorm.clonestagram.util.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/feeds")
public class FeedController {

    private final FeedService feedService;

    // ✅ 로그인한 사용자의 피드 조회
    @GetMapping
    public ResponseEntity<Page<FeedResponseDto>> getMyFeed(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(feedService.getUserFeed(userDetails.getId(), pageable));
    }


    // ✅ 전체 피드 조회
    @GetMapping("/all")
    public ResponseEntity<Page<FeedResponseDto>> getAllFeed(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(feedService.getAllFeed(pageable));
    }


    // ✅ 팔로우 피드 조회
    @GetMapping("/follow")
    public ResponseEntity<Page<FeedResponseDto>> getFollowFeed(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(feedService.getFollowFeed(userDetails.getId(), pageable));
    }


    // ✅ 확인한 피드 삭제
    @DeleteMapping("/seen")
    public ResponseEntity<Void> removeSeenFeeds(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid SeenRequest request
    ) {
        feedService.removeSeenFeeds(userDetails.getId(), request.getPostIds());
        return ResponseEntity.noContent().build();
    }


    @DeleteMapping("/all")
    public ResponseEntity<Void> deleteAllFeedsByUser(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        feedService.deleteAllByUser(userDetails.getId());
        return ResponseEntity.noContent().build();
    }
}

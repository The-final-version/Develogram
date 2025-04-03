package com.goorm.clonestagram.feed.controller;

import com.goorm.clonestagram.feed.dto.FeedResponseDto;
import com.goorm.clonestagram.feed.service.FeedService;
import com.goorm.clonestagram.post.dto.PostResDto;
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

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/feeds")
public class FeedController {

    private final FeedService feedService;

    /**
     * ✅ 로그인한 사용자의 피드 조회 (페이징)
     * 예: GET /api/feed?page=0&size=20
     */
    @GetMapping
    public ResponseEntity<Page<FeedResponseDto>> getMyFeed(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<FeedResponseDto> feedPage = feedService.getUserFeed(userDetails.getId(), pageable);
        return ResponseEntity.ok(feedPage);
    }


    @GetMapping("/all")
    public ResponseEntity<Page<FeedResponseDto>> allFeed(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ){
        Page<FeedResponseDto> feedPage = feedService.getAllFeed(pageable);
        return ResponseEntity.ok(feedPage);
    }

    @GetMapping("/follow")
    public  ResponseEntity<Page<FeedResponseDto>> followFeed(@AuthenticationPrincipal CustomUserDetails userDetail,
                                                 @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)Pageable pageable
    ){
        Long userId = userDetail.getId();

        return ResponseEntity.ok(feedService.getFollowFeed(userId, pageable));
    }

    /**
     * ✅ 사용자가 본 게시물 삭제
     * 예: DELETE /api/feed/seen
     * body: { "postIds": [1, 2, 3] }
     */
    @DeleteMapping("/seen")
    public ResponseEntity<Void> removeSeenFeeds(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid SeenRequest request
    ) {
        feedService.removeSeenFeeds(userDetails.getId(), request.getPostIds());
        return ResponseEntity.noContent().build();
    }

    // ✅ Request DTO
    public static class SeenRequest {
        private List<Long> postIds;

        public List<Long> getPostIds() {
            return postIds;
        }

        public void setPostIds(List<Long> postIds) {
            this.postIds = postIds;
        }
    }
}


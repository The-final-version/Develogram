package com.goorm.clonestagram.search.controller;

import com.goorm.clonestagram.search.dto.SearchUserResDto;
import com.goorm.clonestagram.search.service.FollowSearchService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class FollowSearchController {

    private final FollowSearchService followSearchService;

    @GetMapping("users/{userId}/following")
    public ResponseEntity<SearchUserResDto> searchFollowing(
        @PathVariable Long userId,
        @RequestParam @NotBlank String keyword,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(followSearchService.searchFollowingByKeyword(userId, keyword, pageable));
    }

    @GetMapping("users/{userId}/follower")
    public ResponseEntity<SearchUserResDto> searchFollower(
        @PathVariable Long userId,
        @RequestParam @NotBlank String keyword,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(followSearchService.searchFollowerByKeyword(userId, keyword, pageable));
    }

}

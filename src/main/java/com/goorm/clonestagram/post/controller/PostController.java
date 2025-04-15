package com.goorm.clonestagram.post.controller;

import com.goorm.clonestagram.post.dto.PostResDto;
import com.goorm.clonestagram.post.service.PostService;
import com.goorm.clonestagram.like.service.LikeService;
import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.service.UserExternalQueryService;
import com.goorm.clonestagram.util.CustomUserDetails;
import com.goorm.clonestagram.post.domain.Posts;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.goorm.clonestagram.post.dto.PostInfoDto;

@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService feedService;
    private final LikeService likeService;
    private final PostService postService;
    private final UserExternalQueryService userService;     // 유저 도메인 수정

    @GetMapping("/feeds/user")
    public ResponseEntity<PostResDto> userPosts(@RequestParam("userId") Long userId,
                                               @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        User user = userService.findByIdAndDeletedIsFalse(userId); // 유저 도메인 수정
        if (user == null) {
            throw new IllegalArgumentException("해당 유저를 찾을 수 없습니다.");
        }
        return ResponseEntity.ok(postService.getMyPosts(userId, pageable));
    }

    @GetMapping("/posts/{postId}")
    public ResponseEntity<PostInfoDto> getPostById(@PathVariable("postId")  Long postId) {
        Posts post = postService.findByIdAndDeletedIsFalse(postId);
        return ResponseEntity.ok(PostInfoDto.fromEntity(post));
    }

}

package com.goorm.clonestagram.post.controller;

import com.goorm.clonestagram.post.dto.PostResDto;
import com.goorm.clonestagram.post.service.PostService;
import com.goorm.clonestagram.like.service.LikeService;
import com.goorm.clonestagram.util.CustomUserDetails;
import com.goorm.clonestagram.user.service.UserService;
import com.goorm.clonestagram.user.domain.Users;
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
    private final UserService userService;

    @GetMapping("/feeds/user")
    public ResponseEntity<PostResDto> userPosts(@RequestParam("userId") Long userId,
                                               @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Users user = userService.findByIdAndDeletedIsFalse(userId);
        if (user == null) {
            throw new IllegalArgumentException("해당 유저를 찾을 수 없습니다.");
        }
        return ResponseEntity.ok(postService.getMyPosts(userId, pageable));
    }

    @GetMapping("/api/posts/{postId}")
    public ResponseEntity<PostInfoDto> getPostById(@PathVariable Long postId) {
        Posts post = postService.findByIdAndDeletedIsFalse(postId);
        return ResponseEntity.ok(PostInfoDto.fromEntity(post));
    }

}

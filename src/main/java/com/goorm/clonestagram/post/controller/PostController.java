package com.goorm.clonestagram.post.controller;

import com.goorm.clonestagram.post.dto.PostResDto;
import com.goorm.clonestagram.post.service.PostService;
import com.goorm.clonestagram.like.service.LikeService;
import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.service.UserExternalQueryService;
import com.goorm.clonestagram.util.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService feedService;
    private final LikeService likeService;
    private final PostService postService;
    private final UserExternalQueryService userService;

    @GetMapping("/feeds/user")
    public ResponseEntity<PostResDto> userPosts(@RequestParam("userId") Long userId,
                                               @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        userService.findByIdAndDeletedIsFalse(userId);
        return ResponseEntity.ok(postService.getMyPosts(userId, pageable));
    }

}

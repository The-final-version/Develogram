package com.goorm.clonestagram.user.presentation.controller.profile;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.goorm.clonestagram.user.application.adapter.UserAdapter;
import com.goorm.clonestagram.user.application.dto.profile.UserProfileDto;
import com.goorm.clonestagram.user.domain.service.UserInternalQueryService;
import com.goorm.clonestagram.util.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserInternalQueryService userQueryService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> getMyProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(UserAdapter.toUserProfileDto(userDetails.getUser()));
    }

    @GetMapping("/id")
    public ResponseEntity<String> getUserIdByname(@RequestParam("name") String name) {
        Long userId = userQueryService.findUserIdByname(name);
        return ResponseEntity.ok(userId.toString());
    }
}

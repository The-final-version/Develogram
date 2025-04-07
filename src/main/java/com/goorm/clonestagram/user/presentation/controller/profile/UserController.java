package com.goorm.clonestagram.user.presentation.controller.profile;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.goorm.clonestagram.user.application.adapter.UsersAdapter;
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
        if (userDetails == null || userDetails.getUser() == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(UsersAdapter.toUserProfileDto(userDetails.getUser()));
    }

    @GetMapping("/id")
    public ResponseEntity<String> getUserIdByUsername(@RequestParam("username") String username) {
        Long userId = userQueryService.findUserIdByUsername(username);
        return ResponseEntity.ok(userId.toString());
    }
}

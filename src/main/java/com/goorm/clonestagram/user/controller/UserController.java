// com.goorm.clonestagram.user.controller.UserController.java

package com.goorm.clonestagram.user.controller;

import com.goorm.clonestagram.user.domain.Users;
import com.goorm.clonestagram.user.dto.UserProfileDto;
import com.goorm.clonestagram.user.service.UserService;
import com.goorm.clonestagram.util.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> getMyProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getUser() == null) {
            return ResponseEntity.status(401).build();
        }

        Users users = userDetails.getUser();
        return ResponseEntity.ok(UserProfileDto.fromEntity(users));
    }

    @GetMapping("/id")
    public ResponseEntity<String> getUserIdByUsername(@RequestParam("username") String username) {
        Long userId = userService.findUserIdByUsername(username);
        return ResponseEntity.ok(userId.toString());
    }
}

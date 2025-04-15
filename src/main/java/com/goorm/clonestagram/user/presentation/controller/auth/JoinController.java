package com.goorm.clonestagram.user.presentation.controller.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.goorm.clonestagram.user.application.dto.auth.JoinDto;
import com.goorm.clonestagram.user.application.service.auth.UserJoinService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class JoinController {
    private final UserJoinService userJoinService;

    // 회원 가입 처리 (REST API 방식)
    @PostMapping("/join")
    public ResponseEntity<Object> join(@Valid @RequestBody JoinDto joinDto) {
        userJoinService.joinProcess(joinDto);
        return new ResponseEntity<>("회원가입 성공", HttpStatus.CREATED);
    }
}

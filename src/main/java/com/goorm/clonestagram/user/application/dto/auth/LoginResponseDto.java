package com.goorm.clonestagram.user.application.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // ← 직렬화에 꼭 필요
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponseDto {
    private String message;
    private String userId;
    private String accessToken;
    private String refreshToken;
}

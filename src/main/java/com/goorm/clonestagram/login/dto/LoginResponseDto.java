package com.goorm.clonestagram.login.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // ← 직렬화에 꼭 필요
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponseDto {
    private String message;
    private String userId;
}
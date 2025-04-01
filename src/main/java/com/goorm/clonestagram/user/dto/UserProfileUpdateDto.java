package com.goorm.clonestagram.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 사용자 프로필 수정에 필요한 데이터를 담고 있는 DTO 클래스
 * - 사용자가 프로필을 수정할 때 요청으로 전달되는 데이터를 관리
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateDto {
    private Long id;
    private String bio; // 자기소개
    private String profileImage; // 프로필 이미지


}

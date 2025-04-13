package com.goorm.clonestagram.post.dto.upload;

import com.goorm.clonestagram.post.ContentType;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.user.domain.Users;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 이미지 업로드 요청을 위한 DTO
 * - file, content, type을 클라이언트에게 받음
 * - createdAt을 생성
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ImageUploadReqDto {

    @NotBlank(message = "파일 URL은 필수입니다.")
    private String file;

    @NotBlank(message = "게시물 내용은 필수입니다.")
    private String content;
    private Set<String> hashTagList;

    public Posts toEntity(String imageName, Users user) {
        return Posts.builder()
                .user(user)
                .content(content)
                .mediaName(file)
                .contentType(ContentType.IMAGE)
                .build();
    }
}

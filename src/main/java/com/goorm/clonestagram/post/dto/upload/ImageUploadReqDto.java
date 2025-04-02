package com.goorm.clonestagram.post.dto.upload;

import com.goorm.clonestagram.post.ContentType;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.user.domain.Users;
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

    private String file;
    private String content;
    private List<String> hashTagList;

    public Posts toEntity(String imageName, Users user) {
        return Posts.builder()
                .user(user)
                .content(content)
                .mediaName(file)
                .contentType(ContentType.IMAGE)
                .build();
    }
}

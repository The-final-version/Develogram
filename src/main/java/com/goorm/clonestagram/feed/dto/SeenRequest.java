package com.goorm.clonestagram.feed.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SeenRequest {

    @NotEmpty(message = "삭제할 postId 목록은 비어있을 수 없습니다.")
    private List<Long> postIds;
}

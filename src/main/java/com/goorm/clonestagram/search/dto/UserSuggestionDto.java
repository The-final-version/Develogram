package com.goorm.clonestagram.search.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserSuggestionDto {
    private Long id;
    private String name;
    private String profileimg;
}

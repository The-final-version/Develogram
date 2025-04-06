package com.goorm.clonestagram.search.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class HashtagSuggestionDto {

    private String tagName;
    private long postCount;
}


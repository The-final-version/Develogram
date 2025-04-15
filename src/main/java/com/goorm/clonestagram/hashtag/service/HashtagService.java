package com.goorm.clonestagram.hashtag.service;

import com.goorm.clonestagram.hashtag.entity.HashTags;
import com.goorm.clonestagram.hashtag.entity.PostHashTags;
import com.goorm.clonestagram.hashtag.repository.HashTagRepository;
import com.goorm.clonestagram.hashtag.repository.PostHashTagRepository;
import com.goorm.clonestagram.post.domain.Posts;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HashtagService {

    private final HashTagRepository hashTagRepository;
    private final PostHashTagRepository postHashTagRepository;


    @Transactional
    public void saveHashtags(Posts post, Set<String> hashtagNames) {
        for (String tagContent : hashtagNames) {
            HashTags hashTag = hashTagRepository.findByTagContent(tagContent)
                .orElseGet(() -> hashTagRepository.save(HashTags.builder().tagContent(tagContent).build()));
            postHashTagRepository.save(PostHashTags.builder().posts(post).hashTags(hashTag).build());
        }
    }

}

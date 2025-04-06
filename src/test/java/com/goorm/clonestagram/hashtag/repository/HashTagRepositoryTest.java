package com.goorm.clonestagram.hashtag.repository;

import com.goorm.clonestagram.hashtag.entity.HashTags;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("HashTag 레포지토리 테스트")
class HashTagRepositoryTest {

    @Autowired
    private HashTagRepository hashTagRepository;

    @Test
    @DisplayName("해시태그 저장 성공")
    void save_Success() {
        // given
        HashTags hashTag = new HashTags();
        hashTag.setTagContent("테스트태그");

        // when
        HashTags savedHashTag = hashTagRepository.save(hashTag);

        // then
        assertNotNull(savedHashTag.getId());
        assertEquals("테스트태그", savedHashTag.getTagContent());
    }

    @Test
    @DisplayName("이름으로 해시태그 찾기 성공")
    void findByTagContent_Success() {
        // given
        HashTags hashTag = new HashTags();
        hashTag.setTagContent("테스트태그");
        hashTagRepository.save(hashTag);

        // when
        Optional<HashTags> found = hashTagRepository.findByTagContent("테스트태그");

        // then
        assertTrue(found.isPresent());
        assertEquals("테스트태그", found.get().getTagContent());
    }

    @Test
    @DisplayName("이름 포함하는 해시태그 검색 성공")
    void findByTagContentContaining_Success() {
        // given
        HashTags hashTag1 = new HashTags();
        hashTag1.setTagContent("테스트1");
        
        HashTags hashTag2 = new HashTags();
        hashTag2.setTagContent("테스트2");
        
        HashTags hashTag3 = new HashTags();
        hashTag3.setTagContent("다른태그");
        
        hashTagRepository.save(hashTag1);
        hashTagRepository.save(hashTag2);
        hashTagRepository.save(hashTag3);

        // when
        List<HashTags> foundTags = hashTagRepository.findByTagContentContaining("테스트");

        // then
        assertEquals(2, foundTags.size());
        assertTrue(foundTags.stream()
                .allMatch(tag -> tag.getTagContent().contains("테스트")));
    }
} 
package com.goorm.clonestagram.hashtag.repository;

import com.goorm.clonestagram.hashtag.entity.HashTags;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HashTagRepository extends JpaRepository<HashTags, Long> {
	Optional<HashTags> findByTagContent(String hashTag);

	List<HashTags> findByTagContentContaining(String keyword);
}

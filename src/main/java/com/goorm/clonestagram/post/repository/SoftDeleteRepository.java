package com.goorm.clonestagram.post.repository;

import com.goorm.clonestagram.post.domain.SoftDelete;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SoftDeleteRepository extends JpaRepository<SoftDelete, Long> {
	List<SoftDelete> findByDeletedAtBefore(LocalDateTime threshold);
}

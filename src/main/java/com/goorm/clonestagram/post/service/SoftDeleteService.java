package com.goorm.clonestagram.post.service;

import org.springframework.stereotype.Service;

import com.goorm.clonestagram.post.EntityType;
import com.goorm.clonestagram.post.domain.SoftDelete;
import com.goorm.clonestagram.post.repository.SoftDeleteRepository;
import com.goorm.clonestagram.user.domain.entity.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SoftDeleteService {
	private final SoftDeleteRepository softDeleteRepository;

	/**
	 * 소프트 삭제된 게시글을 삭제합니다.
	 */
	public void saveUserSoftDeleteRecord(User user) {
		softDeleteRepository.save(new SoftDelete(null, EntityType.USER, user.getId(), user.getDeletedAt()));
	}
}

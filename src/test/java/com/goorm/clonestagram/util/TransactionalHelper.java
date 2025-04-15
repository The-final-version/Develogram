package com.goorm.clonestagram.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.goorm.clonestagram.like.service.LikeService;

@Service
public class TransactionalHelper {

	@Autowired
	private LikeService likeService;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public boolean checkLiked(Long userId, Long postId) {
		return likeService.isPostLikedByLoginUser(userId, postId);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public long getLikeCount(Long postId) {
		return likeService.getLikeCount(postId);
	}
}

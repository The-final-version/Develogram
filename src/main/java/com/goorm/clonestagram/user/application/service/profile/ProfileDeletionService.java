package com.goorm.clonestagram.user.application.service.profile;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goorm.clonestagram.post.EntityType;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.post.domain.SoftDelete;
import com.goorm.clonestagram.post.repository.PostsRepository;
import com.goorm.clonestagram.post.repository.SoftDeleteRepository;
import com.goorm.clonestagram.post.service.PostService;
import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.service.UserInternalQueryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProfileDeletionService {
	private final UserInternalQueryService userInternalQueryService;
	private final PostService postService;

	/**
	 * (public) 사용자 프로필 삭제
	 * @param userId 삭제할 사용자의 ID
	 * @throws IllegalArgumentException 사용자가 존재하지 않으면 예외 발생
	 */
	@Transactional
	public void deleteUserProfile(Long userId) {
		// 1. 사용자 조회
		User user = userInternalQueryService.findUserById(userId);

		// 2. 연결된 게시글 모두 삭제 처리
		deleteAllUserPosts(user.getId());

		// 3. 사용자 삭제 처리
		user.delete();

		// 4. 소프트 삭제 기록 저장
		saveUserSoftDeleteRecord(user);
	}

	/* =============================
       ( Private Methods )
   	============================= */

	/**
	 * 사용자 프로필에 연결된 모든 게시글을 삭제 처리합니다.
	 * TODO: 추후 PostService로 이동해야 함.
	 */
	private final PostsRepository postsRepository;
	private void deleteAllUserPosts(Long userId) {
		List<Posts> posts = postService.findAllByUserIdAndDeletedIsFalse(userId);
		for (Posts post : posts) {
			post.setDeleted(true);
			post.setDeletedAt(LocalDateTime.now());
		}
		postsRepository.saveAll(posts);
	}

	/**
	 * 사용자 소프트 삭제 기록을 저장합니다.
	 * TODO: 추후 SoftDeleteService로 이동해야 함.
	 */
	private final SoftDeleteRepository softDeleteRepository;
	private void saveUserSoftDeleteRecord(User user) {
		softDeleteRepository.save(new SoftDelete(null, EntityType.USER, user.getId(), user.getDeletedAt()));
		userInternalQueryService.saveUser(user);
	}
}

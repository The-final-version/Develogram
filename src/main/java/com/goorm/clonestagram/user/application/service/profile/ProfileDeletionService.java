package com.goorm.clonestagram.user.application.service.profile;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goorm.clonestagram.post.service.PostService;
import com.goorm.clonestagram.post.service.SoftDeleteService;
import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.service.UserInternalQueryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProfileDeletionService {
	private final UserInternalQueryService userInternalQueryService;
	private final PostService postService;
	private final SoftDeleteService softDeleteService;
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
		postService.deleteAllUserPosts(user.getId());

		// 3. 사용자 삭제 처리 진행
		user.delete();

		// 4. 소프트 삭제 기록 저장
		softDeleteService.saveUserSoftDeleteRecord(user);

		// 5. 사용자 삭제 처리 저장
		userInternalQueryService.saveUser(user);
	}
}

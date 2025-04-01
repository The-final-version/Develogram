package com.goorm.clonestagram.comment.service;
import com.goorm.clonestagram.comment.domain.CommentEntity;
import com.goorm.clonestagram.comment.dto.CommentRequest;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.post.repository.PostsRepository;
import com.goorm.clonestagram.comment.repository.CommentRepository;
import com.goorm.clonestagram.user.domain.User;
import com.goorm.clonestagram.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final PostsRepository postsRepository;


    @Transactional
    public CommentEntity createComment(CommentEntity comment) {

        // ✅ userId가 존재하는지 확인
        if (!userRepository.existsByIdAndDeletedIsFalse(comment.getUser().getId())) {
            throw new IllegalArgumentException("존재하지 않는 사용자 ID입니다: " + comment.getUser().getId());
        }

        // ✅ postId가 존재하는지 확인
        if (!postsRepository.existsByIdAndDeletedIsFalse(comment.getPosts().getId())) {
            throw new IllegalArgumentException("존재하지 않는 게시글 ID입니다: " + comment.getPosts().getId());
        }

        return commentRepository.save(comment);
    }

    @Transactional
    public CommentEntity createCommentWithRollback(CommentRequest commentRequest) {
        User user = userRepository.findByIdAndDeletedIsFalse(commentRequest.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("유저가 없습니다"));
        Posts posts = postsRepository.findByIdAndDeletedIsFalse(commentRequest.getPostId())
                .orElseThrow(() -> new IllegalArgumentException("피드가 없습니다"));

        return commentRepository.save(CommentEntity.builder()
                .user(user)
                .posts(posts)
                .content(commentRequest.getContent())
                .build());
    }

    @Transactional(readOnly = true)
    public CommentEntity getCommentById(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 댓글이 존재하지 않습니다: " + id));
    }

    @Transactional(readOnly = true)
    public List<CommentEntity> getCommentsByPostId(Long postId) {
        List<CommentEntity> comments = commentRepository.findByPostsId(postId);

//        // ❌ 현재 이렇게 되어 있을 가능성 있음
//        if (comments.isEmpty()) {
//            throw new IllegalArgumentException("해당 게시글(" + postId + ")에는 댓글이 없습니다.");
//        }

        // ✅ 아래처럼 수정
        return comments; // 비어있더라도 그대로 리턴
    }

    @Transactional
    public void removeComment(Long commentId, Long requesterId) {
        // 1️⃣ 댓글 존재 여부 확인
        CommentEntity comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글 ID입니다: " + commentId));

        // 2️⃣ 해당 댓글이 속한 게시글 조회 (✔ postId 사용)
        Long postId = comment.getPosts().getId(); // ✅ comment에서 postId 가져오기
        Posts post = postsRepository.findByIdAndDeletedIsFalse(postId) // ✅ postId 기반 조회
                .orElseThrow(() -> new IllegalArgumentException("댓글이 속한 게시글이 존재하지 않습니다. postId: " + postId));

        // 3️⃣ 댓글 삭제 권한 확인 (댓글 작성자 또는 게시글 작성자)
        if (!comment.getUser().getId().equals(requesterId) && !post.getUser().getId().equals(requesterId)) {
            throw new IllegalArgumentException("댓글을 삭제할 권한이 없습니다.");
        }

        // 4️⃣ 삭제 전 로그 출력
        log.info("🗑️ 댓글 삭제 요청 - Comment ID: {}, 요청자 ID: {}", commentId, requesterId);

        // 5️⃣ 삭제 실행 (flush() 제거)
        commentRepository.deleteById(commentId);

        log.info("✅ 댓글 삭제 완료 - Comment ID: {}", commentId);
    }

}

package com.goorm.clonestagram.service;

import com.goorm.clonestagram.entity.CommentEntity;
import com.goorm.clonestagram.entity.CommentRepository;
import com.goorm.clonestagram.entity.PostRepository;
import com.goorm.clonestagram.entity.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

@RequiredArgsConstructor
@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    @Transactional
    public CommentEntity createComment(@RequestBody CommentEntity comment) {

        // ✅ userId가 존재하는지 확인
        if (!userRepository.existsById(comment.getUserId())) {
            throw new IllegalArgumentException("존재하지 않는 사용자 ID입니다: " + comment.getUserId());
        }

        // ✅ postId가 존재하는지 확인
        if (!postRepository.existsById(comment.getPostId())) {
            throw new IllegalArgumentException("존재하지 않는 게시글 ID입니다: " + comment.getPostId());
        }

        return commentRepository.save(comment);
    }


    @Transactional
    public CommentEntity createCommentWithRollback(CommentEntity entity) {
        return commentRepository.save(entity);
    }


    @Transactional(readOnly = true)
    public CommentEntity getCommentById(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 댓글이 존재하지 않습니다: " + id));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ)
    public CommentEntity updateComment(Long id, CommentEntity comment) {
        CommentEntity commentEntity = commentRepository.findById(id).orElseThrow(() -> new RuntimeException("Comment not found"));
        commentEntity.setContent(comment.getContent());
        return commentRepository.save(commentEntity);

    }

}

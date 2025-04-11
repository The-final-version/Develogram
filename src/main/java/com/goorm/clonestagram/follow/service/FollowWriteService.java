package com.goorm.clonestagram.follow.service;

import com.goorm.clonestagram.follow.domain.Follows;
import com.goorm.clonestagram.follow.repository.FollowRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FollowWriteService {

    private final FollowRepository followRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void tryCreate(Follows follows) {
        try {
            followRepository.save(follows);
        } catch (DataIntegrityViolationException e) {
            log.warn("중복 팔로우 무시: followerId={}, followedId={}",
                    follows.getFollower().getId(), follows.getFollowed().getId());
            entityManager.detach(follows);
            entityManager.clear(); // 세션 완전 정리
        }
    }
}

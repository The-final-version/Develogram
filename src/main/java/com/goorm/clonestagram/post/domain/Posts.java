package com.goorm.clonestagram.post.domain;

import com.goorm.clonestagram.comment.domain.Comments;
import com.goorm.clonestagram.post.ContentType;
import com.goorm.clonestagram.like.domain.Like;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;

import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Posts 엔티티
 * - 이미지, 컨텐츠 정보를 담는 테이블 매핑 클래스
 * - media 파일의 경로, 작성 내용, 작성 시간 등을 포함
 */
@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(name = "posts")
public class Posts {

	public Posts(Long id) {
		this.id = id;
	}

	/**
	 * 게시물 Primary Key
	 * - 자동 증가 (IDENTITY 전략)
	 */
	@Id
	@Column(name = "id", nullable = false)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private UserEntity user;

	/**
	 * 게시물 내용
	 * - 이미지에 대한 설명 또는 글 내용
	 */
	@Column(name = "content", columnDefinition = "TEXT")
	private String content;

	/**
	 * 미디어 파일명
	 * - unique한 파일명이기에 select시 사용 가능
	 */
	@Column(name = "media_name")
	private String mediaName;

	/**
	 * 콘텐츠 타입
	 * - 이미지, 동영상 타입 구분
	 * - Enum으로 관리
	 */
	@Column(name = "contents_type")
	@Enumerated(EnumType.STRING)
	private ContentType contentType;

	/**
	 * 게시물 생성 시간
	 * - imageUploadReqDto.toEntity()에서 셋팅
	 */
	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	/**
	 * 게시물 수정 시간
	 * - 게시물이 업데이트 될 때 변경됨
	 * - 처음 생성 시 null 가능
	 */
	@UpdateTimestamp
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@Column(name = "deleted_yn", nullable = false)
	private boolean deleted = false;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	@OneToMany(mappedBy = "post", cascade = CascadeType.REMOVE)
	private List<Like> likes;

	@OneToMany(mappedBy = "posts", cascade = CascadeType.REMOVE)
	private List<Comments> comments;

	@Version
	private Long version;

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}

}

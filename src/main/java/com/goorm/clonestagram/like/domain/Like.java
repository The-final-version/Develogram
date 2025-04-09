package com.goorm.clonestagram.like.domain;

import jakarta.persistence.*;
import lombok.*;

import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "likes")
public class Like {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private UserEntity user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "post_id", nullable = false)
	private Posts post;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@PrePersist
	protected void onCreate() {
		this.createdAt = LocalDateTime.now();
	}

	public Like(UserEntity user, Posts post) {
		this.user = user;
		this.post = post;
		this.createdAt = LocalDateTime.now();
	}
}

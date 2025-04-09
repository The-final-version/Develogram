package com.goorm.clonestagram.hashtag.entity;

import com.goorm.clonestagram.post.domain.Posts;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "post_hashtags")
@NoArgsConstructor
@AllArgsConstructor
public class PostHashTags {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "hashtag_id")
	private HashTags hashTags;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "post_id")
	private Posts posts;
}

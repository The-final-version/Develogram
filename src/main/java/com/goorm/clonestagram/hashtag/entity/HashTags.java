package com.goorm.clonestagram.hashtag.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "hashtags")
@AllArgsConstructor
@NoArgsConstructor
public class HashTags {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "tag_content", nullable = false)
	private String tagContent;

}

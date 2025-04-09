package com.goorm.clonestagram.post.domain;

import com.goorm.clonestagram.post.EntityType;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "soft_delete")
@NoArgsConstructor
@AllArgsConstructor
public class SoftDelete {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "entity_type")
	private EntityType entityType;

	@Column(name = "entity_id")
	private Long entityId;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;
}



package com.goorm.clonestagram.common.base;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
@Getter
public abstract class BaseEntity {

	@CreatedBy
	@Column(updatable = false)
	private String createdBy;

	@LastModifiedBy
	@Column
	private String modifiedBy;

	@CreationTimestamp
	@Column(updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column
	private LocalDateTime updatedAt;

	@Column(name = "deleted", nullable = false)
	private Boolean deleted = false;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	public void delete() {
		this.deleted = true;
		this.deletedAt = LocalDateTime.now();
	}
}

package com.junhong.somin.media;

import com.junhong.somin.common.KoreaTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "media_comments")
public class MediaComment {

	@Id
	private String id;

	@Column(length = 36, nullable = false)
	private String mediaItemId;

	@Column(length = 500, nullable = false)
	private String content;

	@Column(length = 40)
	private String createdBy;

	@Column(length = 40)
	private String creatorNickname;

	private LocalDateTime createdAt;

	protected MediaComment() {
	}

	public MediaComment(String mediaItemId, String content, String createdBy, String creatorNickname) {
		this.id = UUID.randomUUID().toString();
		this.mediaItemId = mediaItemId;
		this.content = content;
		this.createdBy = createdBy;
		this.creatorNickname = creatorNickname;
	}

	@PrePersist
	void prePersist() {
		createdAt = KoreaTime.now();
	}

	public String getId() {
		return id;
	}

	public String getMediaItemId() {
		return mediaItemId;
	}

	public String getContent() {
		return content;
	}

	public void updateContent(String content) {
		this.content = content;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public String getCreatorNickname() {
		return creatorNickname;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}

package com.junhong.somin.media;

import com.junhong.somin.common.KoreaTime;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "media_items")
public class MediaItem {

	@Id
	private String id;

	private String eventId;

	@Enumerated(EnumType.STRING)
	private MediaType mediaType;

	@Column(length = 1000)
	private String url;
	@Column(length = 255)
	private String originalFileName;
	@Column(length = 120)
	private String title;
	@Column(length = 2000)
	private String memo;
	private boolean favorite;
	private LocalDate capturedAt;
	@Column(length = 40)
	private String createdBy;
	@Column(length = 40)
	private String creatorNickname;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	protected MediaItem() {
	}

	public MediaItem(
			String eventId,
			MediaType mediaType,
			String url,
			String originalFileName,
			String title,
			String memo,
			LocalDate capturedAt,
			String createdBy,
			String creatorNickname) {
		this.id = UUID.randomUUID().toString();
		this.eventId = eventId;
		this.mediaType = mediaType;
		this.url = url;
		this.originalFileName = originalFileName;
		this.title = title;
		this.memo = memo;
		this.capturedAt = capturedAt;
		this.createdBy = createdBy;
		this.creatorNickname = creatorNickname;
	}

	@PrePersist
	void prePersist() {
		LocalDateTime now = KoreaTime.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = KoreaTime.now();
	}

	public String getId() {
		return id;
	}

	public String getEventId() {
		return eventId;
	}

	public void setEventId(String eventId) {
		this.eventId = eventId;
	}

	public MediaType getMediaType() {
		return mediaType;
	}

	public String getUrl() {
		return url;
	}

	public String getOriginalFileName() {
		return originalFileName;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getMemo() {
		return memo;
	}

	public void setMemo(String memo) {
		this.memo = memo;
	}

	public boolean isFavorite() {
		return favorite;
	}

	public void setFavorite(boolean favorite) {
		this.favorite = favorite;
	}

	public LocalDate getCapturedAt() {
		return capturedAt;
	}

	public void setCapturedAt(LocalDate capturedAt) {
		this.capturedAt = capturedAt;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public String getCreatorNickname() {
		return creatorNickname;
	}

	public void setCreatorNickname(String creatorNickname) {
		this.creatorNickname = creatorNickname;
	}

	public void touch() {
		updatedAt = KoreaTime.now();
	}
}

package com.junhong.somin.profile;

import com.junhong.somin.common.KoreaTime;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "couple_profile")
public class CoupleProfile {

	@Id
	private String id;

	private String boyfriendName;
	private String girlfriendName;
	private LocalDate relationshipStartDate;
	private String coverPhotoUrl;
	private String splashPhotoUrl;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	protected CoupleProfile() {
	}

	public CoupleProfile(String id, String boyfriendName, String girlfriendName, LocalDate relationshipStartDate) {
		this.id = id;
		this.boyfriendName = boyfriendName;
		this.girlfriendName = girlfriendName;
		this.relationshipStartDate = relationshipStartDate;
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

	public String getBoyfriendName() {
		return boyfriendName;
	}

	public void setBoyfriendName(String boyfriendName) {
		this.boyfriendName = boyfriendName;
	}

	public String getGirlfriendName() {
		return girlfriendName;
	}

	public void setGirlfriendName(String girlfriendName) {
		this.girlfriendName = girlfriendName;
	}

	public LocalDate getRelationshipStartDate() {
		return relationshipStartDate;
	}

	public void setRelationshipStartDate(LocalDate relationshipStartDate) {
		this.relationshipStartDate = relationshipStartDate;
	}

	public String getCoverPhotoUrl() {
		return coverPhotoUrl;
	}

	public void setCoverPhotoUrl(String coverPhotoUrl) {
		this.coverPhotoUrl = coverPhotoUrl;
	}

	public String getSplashPhotoUrl() {
		return splashPhotoUrl;
	}

	public void setSplashPhotoUrl(String splashPhotoUrl) {
		this.splashPhotoUrl = splashPhotoUrl;
	}
}

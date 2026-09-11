package com.junhong.somin.googlecalendar;

import com.junhong.somin.common.KoreaTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "google_calendar_connections")
public class GoogleCalendarConnection {

	@Id
	@Column(length = 40)
	private String username;

	@Column(columnDefinition = "text", nullable = false)
	private String encryptedAccessToken;

	@Column(columnDefinition = "text", nullable = false)
	private String encryptedRefreshToken;

	@Column(nullable = false)
	private LocalDateTime accessTokenExpiresAt;

	@Column(length = 1024)
	private String googleCalendarId;

	@Column(length = 1000)
	private String grantedScope;

	@Column(nullable = false)
	private LocalDateTime connectedAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	protected GoogleCalendarConnection() {
	}

	public GoogleCalendarConnection(
			String username,
			String encryptedAccessToken,
			String encryptedRefreshToken,
			LocalDateTime accessTokenExpiresAt,
			String grantedScope) {
		this.username = username;
		this.encryptedAccessToken = encryptedAccessToken;
		this.encryptedRefreshToken = encryptedRefreshToken;
		this.accessTokenExpiresAt = accessTokenExpiresAt;
		this.grantedScope = grantedScope;
	}

	@PrePersist
	void prePersist() {
		LocalDateTime now = KoreaTime.now();
		connectedAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = KoreaTime.now();
	}

	public String getUsername() {
		return username;
	}

	public String getEncryptedAccessToken() {
		return encryptedAccessToken;
	}

	public String getEncryptedRefreshToken() {
		return encryptedRefreshToken;
	}

	public LocalDateTime getAccessTokenExpiresAt() {
		return accessTokenExpiresAt;
	}

	public LocalDateTime getConnectedAt() {
		return connectedAt;
	}

	public String getGoogleCalendarId() {
		return googleCalendarId;
	}

	public String getGrantedScope() {
		return grantedScope;
	}

	public void updateTokens(
			String encryptedAccessToken,
			String encryptedRefreshToken,
			LocalDateTime accessTokenExpiresAt,
			String grantedScope) {
		this.encryptedAccessToken = encryptedAccessToken;
		if (encryptedRefreshToken != null && !encryptedRefreshToken.isBlank()) {
			this.encryptedRefreshToken = encryptedRefreshToken;
		}
		this.accessTokenExpiresAt = accessTokenExpiresAt;
		if (grantedScope != null && !grantedScope.isBlank()) {
			this.grantedScope = grantedScope;
		}
	}

	public void setGoogleCalendarId(String googleCalendarId) {
		this.googleCalendarId = googleCalendarId;
	}
}

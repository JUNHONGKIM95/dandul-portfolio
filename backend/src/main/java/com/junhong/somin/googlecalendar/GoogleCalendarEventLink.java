package com.junhong.somin.googlecalendar;

import com.junhong.somin.common.KoreaTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
		name = "google_calendar_event_links",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_google_calendar_event_link",
				columnNames = {"username", "date_event_id"}))
public class GoogleCalendarEventLink {

	@Id
	private String id;

	@Column(length = 40, nullable = false)
	private String username;

	@Column(name = "date_event_id", length = 36, nullable = false)
	private String dateEventId;

	@Column(length = 1024, nullable = false)
	private String googleCalendarId;

	@Column(length = 1024, nullable = false)
	private String googleEventId;

	@Column(nullable = false)
	private LocalDateTime syncedAt;

	protected GoogleCalendarEventLink() {
	}

	public GoogleCalendarEventLink(
			String username,
			String dateEventId,
			String googleCalendarId,
			String googleEventId) {
		this.id = UUID.randomUUID().toString();
		this.username = username;
		this.dateEventId = dateEventId;
		this.googleCalendarId = googleCalendarId;
		this.googleEventId = googleEventId;
	}

	@PrePersist
	@PreUpdate
	void markSynced() {
		syncedAt = KoreaTime.now();
	}

	public String getUsername() {
		return username;
	}

	public String getDateEventId() {
		return dateEventId;
	}

	public String getGoogleEventId() {
		return googleEventId;
	}

	public String getGoogleCalendarId() {
		return googleCalendarId;
	}

	public void updateGoogleEventId(String googleEventId) {
		this.googleEventId = googleEventId;
	}
}

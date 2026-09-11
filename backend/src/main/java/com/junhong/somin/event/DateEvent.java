package com.junhong.somin.event;

import com.junhong.somin.common.KoreaTime;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "date_events")
public class DateEvent {

	@Id
	private String id;

	@Column(name = "event_date")
	private LocalDate date;
	@Column(name = "end_date")
	private LocalDate endDate;
	private LocalTime meetingTime;
	@Column(length = 255)
	private String place;
	@Column(length = 120)
	private String title;
	@Column(length = 2000)
	private String memo;
	@Column(length = 20)
	private String category;
	@Column(length = 40)
	private String createdBy;
	@Column(length = 40)
	private String creatorNickname;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	protected DateEvent() {
	}

	public DateEvent(
			LocalDate date,
			LocalDate endDate,
			LocalTime meetingTime,
			String place,
			String title,
			String memo,
			String category,
			String createdBy,
			String creatorNickname) {
		this.id = UUID.randomUUID().toString();
		this.date = date;
		this.endDate = endDate;
		this.meetingTime = meetingTime;
		this.place = place;
		this.title = title;
		this.memo = memo;
		this.category = category;
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

	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}

	public LocalTime getMeetingTime() {
		return meetingTime;
	}

	public void setMeetingTime(LocalTime meetingTime) {
		this.meetingTime = meetingTime;
	}

	public String getPlace() {
		return place;
	}

	public void setPlace(String place) {
		this.place = place;
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

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
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
}

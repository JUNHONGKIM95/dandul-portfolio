package com.junhong.somin.hiking;

import com.junhong.somin.common.KoreaTime;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "hiking_records")
public class HikingRecord {

	@Id
	private String id;

	@Column(length = 120)
	private String mountainName;
	@Column(length = 255)
	private String location;
	private int elevationMeter;
	private LocalDate climbedAt;
	private Double latitude;
	private Double longitude;
	@Column(length = 20)
	private String source;
	@Column(length = 1000)
	private String memo;
	@Column(length = 40)
	private String createdBy;
	@Column(length = 40)
	private String creatorNickname;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	protected HikingRecord() {
	}

	public HikingRecord(
			String mountainName,
			String location,
			int elevationMeter,
			LocalDate climbedAt,
			Double latitude,
			Double longitude,
			String source,
			String memo,
			String createdBy,
			String creatorNickname) {
		this.id = UUID.randomUUID().toString();
		this.mountainName = mountainName;
		this.location = location;
		this.elevationMeter = elevationMeter;
		this.climbedAt = climbedAt;
		this.latitude = latitude;
		this.longitude = longitude;
		this.source = source;
		this.memo = memo;
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

	public String getMountainName() {
		return mountainName;
	}

	public void setMountainName(String mountainName) {
		this.mountainName = mountainName;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public int getElevationMeter() {
		return elevationMeter;
	}

	public void setElevationMeter(int elevationMeter) {
		this.elevationMeter = elevationMeter;
	}

	public LocalDate getClimbedAt() {
		return climbedAt;
	}

	public void setClimbedAt(LocalDate climbedAt) {
		this.climbedAt = climbedAt;
	}

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getMemo() {
		return memo;
	}

	public void setMemo(String memo) {
		this.memo = memo;
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

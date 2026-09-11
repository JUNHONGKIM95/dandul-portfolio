package com.junhong.somin.visit;

import com.junhong.somin.common.KoreaTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "app_visits")
public class AppVisit {

	@Id
	private String id;

	@Column(length = 40, nullable = false)
	private String username;

	@Column(length = 40)
	private String nickname;

	private LocalDateTime visitedAt;

	protected AppVisit() {
	}

	public AppVisit(String username, String nickname) {
		this.id = UUID.randomUUID().toString();
		this.username = username;
		this.nickname = nickname;
	}

	@PrePersist
	void prePersist() {
		visitedAt = KoreaTime.now();
	}

	public String getUsername() {
		return username;
	}

	public LocalDateTime getVisitedAt() {
		return visitedAt;
	}
}

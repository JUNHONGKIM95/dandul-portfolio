package com.junhong.somin.notification;

import com.junhong.somin.common.KoreaTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
		name = "push_subscriptions",
		indexes = {
				@Index(name = "idx_push_subscriptions_username", columnList = "username")
		})
public class PushSubscription {

	@Id
	private String id;

	@Column(length = 40, nullable = false)
	private String username;

	@Column(length = 40)
	private String nickname;

	@Column(length = 2048, nullable = false, unique = true)
	private String endpoint;

	@Column(length = 255, nullable = false)
	private String p256dh;

	@Column(length = 255, nullable = false)
	private String auth;

	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	protected PushSubscription() {
	}

	public PushSubscription(String username, String nickname, String endpoint, String p256dh, String auth) {
		this.id = UUID.randomUUID().toString();
		this.username = username;
		this.nickname = nickname;
		this.endpoint = endpoint;
		this.p256dh = p256dh;
		this.auth = auth;
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

	public String getUsername() {
		return username;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public String getP256dh() {
		return p256dh;
	}

	public String getAuth() {
		return auth;
	}

	public String getNickname() {
		return nickname;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void update(String username, String nickname, String p256dh, String auth) {
		this.username = username;
		this.nickname = nickname;
		this.p256dh = p256dh;
		this.auth = auth;
	}
}

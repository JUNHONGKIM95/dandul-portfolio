package com.junhong.somin.notification;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public final class PushDtos {

	private PushDtos() {
	}

	public record PublicKeyResponse(boolean enabled, String publicKey) {
	}

	public record PushKeys(
			@NotBlank String p256dh,
			@NotBlank String auth) {
	}

	public record SubscriptionRequest(
			@NotBlank @Size(max = 40) String username,
			@Size(max = 40) String nickname,
			@NotBlank @Size(max = 2048) String endpoint,
			@Valid PushKeys keys) {
	}

	public record DeleteSubscriptionRequest(
			@NotBlank @Size(max = 2048) String endpoint) {
	}

	public record SubscriptionStatusResponse(
			String username,
			boolean enabled,
			int subscriptionCount,
			LocalDateTime latestUpdatedAt) {
	}

	public record TestNotificationRequest(
			@NotBlank @Size(max = 40) String username) {
	}

	public record TestNotificationResponse(
			String username,
			int attempted,
			int delivered,
			int failed,
			List<String> details) {
	}
}

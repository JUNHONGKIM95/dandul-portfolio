package com.junhong.somin.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.junhong.somin.media.MediaComment;
import com.junhong.somin.media.MediaItem;
import com.junhong.somin.notification.PushDtos.SubscriptionStatusResponse;
import com.junhong.somin.notification.PushDtos.SubscriptionRequest;
import com.junhong.somin.notification.PushDtos.TestNotificationResponse;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PushNotificationService {

	private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

	private final PushSubscriptionRepository repository;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final String publicKey;
	private final String privateKey;
	private final String subject;

	public PushNotificationService(
			PushSubscriptionRepository repository,
			@Value("${app.push.vapid.public-key:}") String publicKey,
			@Value("${app.push.vapid.private-key:}") String privateKey,
			@Value("${app.push.vapid.subject:mailto:dandul@example.com}") String subject) {
		this.repository = repository;
		this.publicKey = publicKey;
		this.privateKey = privateKey;
		this.subject = subject;
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	public boolean isEnabled() {
		return !publicKey.isBlank() && !privateKey.isBlank();
	}

	public String publicKey() {
		return publicKey;
	}

	@Transactional
	public void register(SubscriptionRequest request) {
		if (request.keys() == null) {
			throw new IllegalArgumentException("Push subscription keys are required.");
		}
		PushSubscription subscription = repository.findByEndpoint(request.endpoint())
				.orElseGet(() -> new PushSubscription(
						username(request.username()),
						nickname(request.nickname()),
						request.endpoint(),
						request.keys().p256dh(),
						request.keys().auth()));
		subscription.update(
				username(request.username()),
				nickname(request.nickname()),
				request.keys().p256dh(),
				request.keys().auth());
		repository.save(subscription);
	}

	@Transactional
	public void delete(String endpoint) {
		repository.deleteByEndpoint(endpoint);
	}

	@Transactional
	public void sendMediaCommentNotification(MediaItem mediaItem, MediaComment comment) {
		if (!isEnabled()) {
			log.info("Push notification skipped because VAPID keys are not configured.");
			return;
		}

		String author = username(comment.getCreatedBy());
		String title = "%s님이 댓글을 남겼어요".formatted(nickname(comment.getCreatorNickname()));
		String mediaTitle = mediaItem.getTitle() == null || mediaItem.getTitle().isBlank()
				? "앨범 사진"
				: mediaItem.getTitle();
		String body = "%s: %s".formatted(mediaTitle, comment.getContent());
		String url = "/?view=album&mediaId=%s".formatted(mediaItem.getId());
		String payload = payload(title, body, url, "comment-%s".formatted(comment.getId()));

		repository.findByUsernameNot(author).forEach(subscription -> send(subscription, payload));
	}

	@Transactional(readOnly = true)
	public SubscriptionStatusResponse status(String username) {
		String normalizedUsername = username(username);
		List<PushSubscription> subscriptions = repository.findByUsername(normalizedUsername);
		LocalDateTime latestUpdatedAt = subscriptions.stream()
				.map(PushSubscription::getUpdatedAt)
				.filter(updatedAt -> updatedAt != null)
				.max(LocalDateTime::compareTo)
				.orElse(null);
		return new SubscriptionStatusResponse(
				normalizedUsername,
				isEnabled() && !subscriptions.isEmpty(),
				subscriptions.size(),
				latestUpdatedAt);
	}

	@Transactional
	public TestNotificationResponse sendTest(String username) {
		if (!isEnabled()) {
			return new TestNotificationResponse(username(username), 0, 0, 0, List.of("VAPID keys are not configured."));
		}
		String normalizedUsername = username(username);
		String payload = payload(
				"DANDUL 알림 테스트",
				"댓글 알림을 받을 준비가 되었어요.",
				"/",
				"push-test-%s".formatted(normalizedUsername));
		int delivered = 0;
		int failed = 0;
		List<String> details = new ArrayList<>();
		List<PushSubscription> subscriptions = repository.findByUsername(normalizedUsername);
		for (PushSubscription subscription : subscriptions) {
			PushDeliveryResult result = sendWithResult(subscription, payload);
			details.add(result.detail());
			if (result.delivered()) {
				delivered += 1;
			} else {
				failed += 1;
			}
		}
		if (subscriptions.isEmpty()) {
			details.add("No push subscription is registered for this user.");
		}
		return new TestNotificationResponse(normalizedUsername, subscriptions.size(), delivered, failed, details);
	}

	private boolean send(PushSubscription subscription, String payload) {
		return sendWithResult(subscription, payload).delivered();
	}

	private PushDeliveryResult sendWithResult(PushSubscription subscription, String payload) {
		try {
			PushService pushService = new PushService(publicKey, privateKey, subject);
			Notification notification = new Notification(
					subscription.getEndpoint(),
					subscription.getP256dh(),
					subscription.getAuth(),
					payload);
			HttpResponse response = pushService.send(notification, Encoding.AES128GCM);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode >= 200 && statusCode < 300) {
				return new PushDeliveryResult(true, "HTTP %d".formatted(statusCode));
			}
			String responseBody = response.getEntity() == null ? "" : EntityUtils.toString(response.getEntity());
			String detail = "HTTP %d %s".formatted(statusCode, responseBody).trim();
			log.warn("Push notification delivery returned {} for endpoint {}", detail, subscription.getEndpoint());
			if (statusCode == 400 || statusCode == 403 || statusCode == 404 || statusCode == 410) {
				repository.delete(subscription);
			}
			return new PushDeliveryResult(false, detail);
		} catch (GeneralSecurityException ex) {
			log.warn("Push notification setup failed", ex);
			return new PushDeliveryResult(false, "%s: %s".formatted(ex.getClass().getSimpleName(), ex.getMessage()));
		} catch (Exception ex) {
			log.warn("Push notification delivery failed; removing stale subscription if needed", ex);
			return new PushDeliveryResult(false, "%s: %s".formatted(ex.getClass().getSimpleName(), ex.getMessage()));
		}
	}

	private record PushDeliveryResult(boolean delivered, String detail) {
	}

	private String payload(String title, String body, String url, String tag) {
		try {
			return objectMapper.writeValueAsString(Map.of(
					"title", title,
					"body", body,
					"url", url,
					"tag", tag,
					"icon", "/dandul-favicon.svg",
					"badge", "/dandul-favicon.svg"));
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException("Unable to build push payload", ex);
		}
	}

	private String username(String value) {
		return value == null || value.isBlank() ? "junhong" : value;
	}

	private String nickname(String value) {
		return value == null || value.isBlank() ? "준홍" : value;
	}
}

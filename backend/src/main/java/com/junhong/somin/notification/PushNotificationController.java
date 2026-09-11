package com.junhong.somin.notification;

import com.junhong.somin.notification.PushDtos.DeleteSubscriptionRequest;
import com.junhong.somin.notification.PushDtos.PublicKeyResponse;
import com.junhong.somin.notification.PushDtos.SubscriptionRequest;
import com.junhong.somin.notification.PushDtos.SubscriptionStatusResponse;
import com.junhong.somin.notification.PushDtos.TestNotificationRequest;
import com.junhong.somin.notification.PushDtos.TestNotificationResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/push")
public class PushNotificationController {

	private final PushNotificationService service;

	public PushNotificationController(PushNotificationService service) {
		this.service = service;
	}

	@GetMapping("/public-key")
	public PublicKeyResponse publicKey() {
		return new PublicKeyResponse(service.isEnabled(), service.publicKey());
	}

	@PostMapping("/subscriptions")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void subscribe(@Valid @RequestBody SubscriptionRequest request) {
		service.register(request);
	}

	@GetMapping("/subscriptions/status")
	public SubscriptionStatusResponse status(@RequestParam String username) {
		return service.status(username);
	}

	@PostMapping("/test")
	public TestNotificationResponse test(@Valid @RequestBody TestNotificationRequest request) {
		return service.sendTest(request.username());
	}

	@DeleteMapping("/subscriptions")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void unsubscribe(@Valid @RequestBody DeleteSubscriptionRequest request) {
		service.delete(request.endpoint());
	}
}

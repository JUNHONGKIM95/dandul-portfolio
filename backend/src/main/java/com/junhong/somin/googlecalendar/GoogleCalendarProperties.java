package com.junhong.somin.googlecalendar;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.google-calendar")
public record GoogleCalendarProperties(
		String clientId,
		String clientSecret,
		String redirectUri,
		String scope,
		String frontendUrl) {

	public boolean configured() {
		return hasText(clientId) && hasText(clientSecret) && hasText(redirectUri);
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}

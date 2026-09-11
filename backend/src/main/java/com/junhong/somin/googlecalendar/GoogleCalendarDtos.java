package com.junhong.somin.googlecalendar;

import java.time.LocalDateTime;
import java.util.List;

public final class GoogleCalendarDtos {

	private GoogleCalendarDtos() {
	}

	public record StatusResponse(
			boolean configured,
			boolean connected,
			boolean requiresReconnect,
			String username,
			String calendarName,
			LocalDateTime connectedAt,
			String message) {
	}

	public record AuthorizationUrlResponse(String url) {
	}

	public record SyncResponse(
			int total,
			int created,
			int updated,
			int deleted,
			int failed,
			List<String> errors) {
	}

	public record ImportResponse(
			int imported,
			int updated,
			int deleted,
			int skipped,
			int failed,
			List<String> errors) {
	}

	public record EventSyncResponse(
			int connected,
			int created,
			int updated,
			int deleted,
			int skipped,
			int failed,
			List<String> errors) {
	}
}

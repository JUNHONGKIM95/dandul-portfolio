package com.junhong.somin.googlecalendar;

import com.junhong.somin.googlecalendar.GoogleCalendarDtos.AuthorizationUrlResponse;
import com.junhong.somin.googlecalendar.GoogleCalendarDtos.EventSyncResponse;
import com.junhong.somin.googlecalendar.GoogleCalendarDtos.ImportResponse;
import com.junhong.somin.googlecalendar.GoogleCalendarDtos.StatusResponse;
import com.junhong.somin.googlecalendar.GoogleCalendarDtos.SyncResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping("/api/google-calendar")
public class GoogleCalendarController {

	private final GoogleCalendarService service;

	public GoogleCalendarController(GoogleCalendarService service) {
		this.service = service;
	}

	@GetMapping("/status")
	public StatusResponse status(@RequestParam String username) {
		return service.status(username);
	}

	@GetMapping("/auth-url")
	public AuthorizationUrlResponse authorizationUrl(@RequestParam String username) {
		return service.authorizationUrl(username);
	}

	@GetMapping("/oauth/callback")
	public RedirectView callback(
			@RequestParam(required = false) String code,
			@RequestParam(required = false) String state,
			@RequestParam(required = false) String error) {
		if (error != null && !error.isBlank()) {
			return new RedirectView(service.frontendRedirect("denied"));
		}
		try {
			service.completeAuthorization(code, state);
			return new RedirectView(service.frontendRedirect("connected"));
		} catch (RuntimeException ex) {
			String message = ex.getMessage() == null ? "connection_failed" : ex.getMessage();
			return new RedirectView(service.frontendRedirect(
					"error:" + URLEncoder.encode(message, StandardCharsets.UTF_8)));
		}
	}

	@PostMapping("/sync")
	public SyncResponse sync(@RequestParam String username) {
		return service.syncAll(username);
	}

	@PostMapping("/import")
	public ImportResponse importEvents(@RequestParam String username) {
		return service.importEvents(username);
	}

	@PostMapping("/sync-event")
	public EventSyncResponse syncEvent(@RequestParam String eventId) {
		return service.syncEventAcrossConnections(eventId);
	}

	@DeleteMapping("/connection")
	public void disconnect(@RequestParam String username) {
		service.disconnect(username);
	}
}

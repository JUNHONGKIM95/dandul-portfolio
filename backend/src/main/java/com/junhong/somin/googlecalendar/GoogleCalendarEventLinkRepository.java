package com.junhong.somin.googlecalendar;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoogleCalendarEventLinkRepository
		extends JpaRepository<GoogleCalendarEventLink, String> {

	Optional<GoogleCalendarEventLink> findByUsernameAndDateEventId(String username, String dateEventId);

	List<GoogleCalendarEventLink> findByUsername(String username);

	List<GoogleCalendarEventLink> findByUsernameAndGoogleCalendarId(
			String username,
			String googleCalendarId);
}

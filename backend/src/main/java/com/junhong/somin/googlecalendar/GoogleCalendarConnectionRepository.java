package com.junhong.somin.googlecalendar;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GoogleCalendarConnectionRepository
		extends JpaRepository<GoogleCalendarConnection, String> {
	@org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
	@org.springframework.data.jpa.repository.Query("select c from GoogleCalendarConnection c where c.username = :username")
	java.util.Optional<GoogleCalendarConnection> findForSync(@org.springframework.data.repository.query.Param("username") String username);
}

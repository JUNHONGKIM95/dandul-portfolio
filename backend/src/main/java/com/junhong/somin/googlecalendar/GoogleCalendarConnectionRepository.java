package com.junhong.somin.googlecalendar;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GoogleCalendarConnectionRepository
		extends JpaRepository<GoogleCalendarConnection, String> {
}

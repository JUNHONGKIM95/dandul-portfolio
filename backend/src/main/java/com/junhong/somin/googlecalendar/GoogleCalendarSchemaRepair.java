package com.junhong.somin.googlecalendar;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class GoogleCalendarSchemaRepair implements ApplicationRunner {

	private final JdbcTemplate jdbcTemplate;

	public GoogleCalendarSchemaRepair(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		jdbcTemplate.execute("""
				alter table google_calendar_connections
				add column if not exists google_calendar_id varchar(1024)
				""");
		jdbcTemplate.execute("""
				alter table google_calendar_connections
				add column if not exists granted_scope varchar(1000)
				""");
		jdbcTemplate.execute("""
				alter table google_calendar_event_links
				add column if not exists google_calendar_id varchar(1024)
				""");
		jdbcTemplate.update("""
				update google_calendar_event_links
				set google_calendar_id = 'primary'
				where google_calendar_id is null
				""");
		jdbcTemplate.execute("""
				alter table google_calendar_event_links
				alter column google_calendar_id set not null
				""");
	}
}

package com.junhong.somin.event;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DateEventRepository extends JpaRepository<DateEvent, String> {
	@Query("""
			select event from DateEvent event
			where event.date <= :end
			and coalesce(event.endDate, event.date) >= :start
			order by event.date asc, event.meetingTime asc
			""")
	List<DateEvent> findOverlappingRangeOrderByDateAscMeetingTimeAsc(
			@Param("start") LocalDate start,
			@Param("end") LocalDate end);

	List<DateEvent> findAllByOrderByDateAscMeetingTimeAsc();
}

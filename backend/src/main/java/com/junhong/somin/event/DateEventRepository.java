package com.junhong.somin.event;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface DateEventRepository extends JpaRepository<DateEvent, String> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select e from DateEvent e where e.category = 'together' and e.meetingTime is not null and e.date between :start and :end order by e.id")
	List<DateEvent> findReminderCandidates(@Param("start") LocalDate start, @Param("end") LocalDate end);
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

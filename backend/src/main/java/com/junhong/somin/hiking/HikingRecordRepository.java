package com.junhong.somin.hiking;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HikingRecordRepository extends JpaRepository<HikingRecord, String> {
	List<HikingRecord> findByClimbedAtBetweenOrderByClimbedAtDesc(LocalDate start, LocalDate end);

	List<HikingRecord> findAllByOrderByClimbedAtDesc();
}

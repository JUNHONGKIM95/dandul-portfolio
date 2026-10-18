package com.junhong.somin.memo;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyMemoRepository extends JpaRepository<DailyMemo, String> {
    List<DailyMemo> findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);
    void deleteByCreatedAtBefore(LocalDateTime before);
}

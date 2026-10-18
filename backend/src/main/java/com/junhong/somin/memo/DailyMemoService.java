package com.junhong.somin.memo;

import com.junhong.somin.common.KoreaTime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DailyMemoService {
    private final DailyMemoRepository repository;
    public DailyMemoService(DailyMemoRepository repository) { this.repository = repository; }

    public record MemoResponse(String id, String createdBy, String creatorNickname, String content, LocalDateTime createdAt) {}
    public record TodayResponse(LocalDate date, String expiresAt, List<MemoResponse> notes) {}

    @Transactional(readOnly = true)
    public TodayResponse today() {
        LocalDate day = KoreaTime.today();
        var notes = repository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                day.atStartOfDay(), day.plusDays(1).atStartOfDay()).stream()
                .map(m -> new MemoResponse(m.getId(), m.getCreatedBy(),
                        "junhong".equals(m.getCreatedBy()) ? "준홍" : "소민", m.getContent(), m.getCreatedAt())).toList();
        return new TodayResponse(day, day.plusDays(1).atStartOfDay().atZone(KoreaTime.zone()).toOffsetDateTime().toString(), notes);
    }

    @Transactional
    public TodayResponse add(String author, String content) {
        if (!"junhong".equals(author) && !"somin".equals(author)) throw new IllegalArgumentException("작성자를 확인해 주세요.");
        if (content == null || content.isBlank() || content.length() > 500) throw new IllegalArgumentException("메모는 1~500자로 입력해 주세요.");
        repository.saveAndFlush(new DailyMemo(author, content.strip(), KoreaTime.now()));
        return today();
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    @Scheduled(fixedDelay = 3600000, initialDelay = 10000)
    @Transactional
    public void clearExpired() {
        repository.deleteByCreatedAtBefore(KoreaTime.today().atStartOfDay());
    }
}

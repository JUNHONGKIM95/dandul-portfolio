package com.junhong.somin.memo;

import com.junhong.somin.common.KoreaTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;
import java.time.LocalDate;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:memos;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false"
})
@Transactional
class DailyMemoServiceTests {
    @Autowired DailyMemoService service;
    @Autowired DailyMemoRepository repository;

    @Test void koreanMidnightHidesPreviousNotesAndCleanupRemovesThem() {
        var day = LocalDate.of(2030, 1, 1);
        try (var clock = mockStatic(KoreaTime.class)) {
            clock.when(KoreaTime::today).thenReturn(day);
            clock.when(KoreaTime::now).thenReturn(day.atTime(23, 59));
            clock.when(KoreaTime::zone).thenReturn(java.time.ZoneId.of("Asia/Seoul"));
            var before = service.add("junhong", " 내일 만나요 ");
            assertThat(before.notes()).hasSize(1);
            assertThat(before.notes().getFirst().content()).isEqualTo("내일 만나요");
            assertThat(before.notes().getFirst().creatorNickname()).isEqualTo("준홍");
            assertThat(before.expiresAt()).isEqualTo("2030-01-02T00:00+09:00");
            clock.when(KoreaTime::today).thenReturn(day.plusDays(1));
            clock.when(KoreaTime::now).thenReturn(day.plusDays(1).atStartOfDay());
            assertThat(service.today().notes()).isEmpty();
            var after = service.add("somin", "좋아요");
            assertThat(after.notes()).hasSize(1);
            assertThat(after.notes().getFirst().creatorNickname()).isEqualTo("소민");
            service.clearExpired();
            assertThat(repository.count()).isEqualTo(1);
        }
    }

    @Test void rejectsInvalidAuthorAndEmptyOrOversizedContent() {
        assertThatThrownBy(() -> service.add("unknown", "내용")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.add("junhong", " ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.add("junhong", "a".repeat(501))).isInstanceOf(IllegalArgumentException.class);
    }
}

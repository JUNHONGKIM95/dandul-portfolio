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

    @Test void returnsOldestFirstSoNewMessagesAppendAtTheBottom() {
        var day = KoreaTime.today();
        repository.save(new DailyMemo("junhong", "세 번째", day.atTime(12, 0)));
        repository.save(new DailyMemo("somin", "첫 번째", day.atTime(9, 0)));
        repository.save(new DailyMemo("junhong", "두 번째", day.atTime(10, 0)));
        assertThat(service.today().notes()).extracting(DailyMemoService.MemoResponse::content)
                .containsExactly("첫 번째", "두 번째", "세 번째");
    }

    @Test void authorCanDeleteOwnMemoAndOtherAuthorsMemoIsPreserved() {
        var own = service.add("junhong", "내 글").notes().getFirst();
        service.add("somin", "상대 글");
        var result = service.delete(own.id(), "junhong");
        assertThat(result.notes()).extracting(DailyMemoService.MemoResponse::content).containsExactly("상대 글");
        assertThat(repository.existsById(own.id())).isFalse();
    }

    @Test void cannotDeleteAnotherAuthorsMemo() {
        var note = service.add("somin", "소민의 글").notes().getFirst();
        assertThatThrownBy(() -> service.delete(note.id(), "junhong"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("본인이 작성한");
        assertThat(repository.existsById(note.id())).isTrue();
    }

    @Test void deleteIsIdempotentWhenMidnightCleanupOrAnotherTabAlreadyRemovedMemo() {
        var note = service.add("junhong", "내 글").notes().getFirst();
        service.delete(note.id(), "junhong");
        assertThat(service.delete(note.id(), "junhong").notes()).isEmpty();
        assertThatThrownBy(() -> service.delete(note.id(), "unknown")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void rejectsInvalidAuthorAndEmptyOrOversizedContent() {
        assertThatThrownBy(() -> service.add("unknown", "내용")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.add("junhong", " ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.add("junhong", "a".repeat(501))).isInstanceOf(IllegalArgumentException.class);
    }
}

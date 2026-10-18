package com.junhong.somin.notification;

import com.junhong.somin.event.DateEvent;
import com.junhong.somin.event.DateEventRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:reminders;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false"
})
@Transactional
class EventReminderServiceTests {
    @Autowired DateEventRepository events;
    @Autowired PushSubscriptionRepository subscriptions;
    @Autowired EventReminderDeliveryRepository deliveries;
    @MockitoBean PushNotificationService push;
    EventReminderService service;
    final LocalDateTime now = LocalDateTime.of(2030, 1, 1, 23, 30);

    @BeforeEach void setup() {
        service = new EventReminderService(events, subscriptions, deliveries, push);
        subscriptions.saveAndFlush(new PushSubscription("junhong", "준홍", "https://push/1", "key", "auth"));
        subscriptions.saveAndFlush(new PushSubscription("somin", "소민", "https://push/2", "key", "auth"));
        when(push.isEnabled()).thenReturn(true);
        when(push.sendEventReminder(any(), any())).thenReturn(true);
    }

    DateEvent event(LocalDateTime time, String category) {
        return events.saveAndFlush(new DateEvent(time.toLocalDate(), time.toLocalDate(), time.toLocalTime(),
                "성수", "같이 저녁", "", category, "junhong", "준홍"));
    }

    @Test void sendsToBothUsersAcrossMidnightOnlyOnce() {
        event(now.plusHours(1), "together");
        service.sendDueReminders(now);
        service.sendDueReminders(now.plusMinutes(1));
        verify(push, times(2)).sendEventReminder(any(), any());
        assertThat(deliveries.count()).isEqualTo(2);
    }

    @Test void excludesPersonalMissingTimePastAndNotYetDue() {
        event(now.plusHours(1).plusSeconds(1), "together");
        event(now, "together");
        event(now.minusMinutes(1), "together");
        event(now.plusMinutes(30), "somin");
        events.saveAndFlush(new DateEvent(now.toLocalDate(), null, null, "서울", "시간 없음", "", "together", "junhong", "준홍"));
        service.sendDueReminders(now);
        verify(push, never()).sendEventReminder(any(), any());
    }

    @Test void retriesOnlyFailedDevice() {
        event(now.plusMinutes(30), "together");
        when(push.sendEventReminder(any(), any())).thenReturn(false, true, true);
        service.sendDueReminders(now);
        assertThat(deliveries.count()).isEqualTo(1);
        service.sendDueReminders(now.plusMinutes(1));
        assertThat(deliveries.count()).isEqualTo(2);
        verify(push, times(3)).sendEventReminder(any(), any());
    }

    @Test void changedMeetingTimeGetsNewReminder() {
        var event = event(now.plusMinutes(30), "together");
        service.sendDueReminders(now);
        event.setDate(LocalDate.of(2030, 1, 2));
        event.setMeetingTime(LocalTime.of(0, 20));
        events.flush();
        service.sendDueReminders(now);
        verify(push, times(4)).sendEventReminder(any(), any());
    }

    @Test void disabledPushDoesNotConsumeReminder() {
        event(now.plusMinutes(30), "together");
        when(push.isEnabled()).thenReturn(false);
        service.sendDueReminders(now);
        assertThat(deliveries.count()).isZero();
        verify(push, never()).sendEventReminder(any(), any());
    }
}

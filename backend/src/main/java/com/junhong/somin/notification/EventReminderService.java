package com.junhong.somin.notification;

import com.junhong.somin.common.KoreaTime;
import com.junhong.somin.event.DateEventRepository;
import java.time.LocalDateTime;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventReminderService {
    private final DateEventRepository events;
    private final PushSubscriptionRepository subscriptions;
    private final EventReminderDeliveryRepository deliveries;
    private final PushNotificationService push;

    public EventReminderService(DateEventRepository events, PushSubscriptionRepository subscriptions,
            EventReminderDeliveryRepository deliveries, PushNotificationService push) {
        this.events = events;
        this.subscriptions = subscriptions;
        this.deliveries = deliveries;
        this.push = push;
    }

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void sendDueReminders() {
        sendDueReminders(KoreaTime.now());
    }

    // Persist a receipt per device and meeting time. Failed devices retry until the meeting.
    void sendDueReminders(LocalDateTime now) {
        if (!push.isEnabled()) return;
        var candidates = events.findReminderCandidates(now.toLocalDate(), now.plusHours(1).toLocalDate());
        for (var event : candidates) {
            var meeting = event.getDate().atTime(event.getMeetingTime());
            if (!meeting.isAfter(now) || meeting.isAfter(now.plusHours(1))) continue;
            for (var subscription : subscriptions.findAll()) {
                if (!"junhong".equals(subscription.getUsername()) && !"somin".equals(subscription.getUsername())) continue;
                String key = event.getId() + ":" + meeting + ":" + subscription.getId();
                if (!deliveries.existsById(key) && push.sendEventReminder(subscription, event)) {
                    deliveries.saveAndFlush(new EventReminderDelivery(key, now));
                }
            }
        }
    }
}

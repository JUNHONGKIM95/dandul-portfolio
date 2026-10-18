package com.junhong.somin.notification;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "event_reminder_deliveries")
public class EventReminderDelivery {
    @Id
    private String id;
    private LocalDateTime deliveredAt;

    protected EventReminderDelivery() {}

    public EventReminderDelivery(String id, LocalDateTime deliveredAt) {
        this.id = id;
        this.deliveredAt = deliveredAt;
    }
}

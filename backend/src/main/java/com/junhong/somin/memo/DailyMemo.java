package com.junhong.somin.memo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "daily_memos")
public class DailyMemo {
    @Id
    private String id;
    @Column(nullable = false, length = 40)
    private String createdBy;
    @Column(nullable = false, length = 500)
    private String content;
    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected DailyMemo() {}

    public DailyMemo(String createdBy, String content, LocalDateTime createdAt) {
        this.id = UUID.randomUUID().toString();
        this.createdBy = createdBy;
        this.content = content;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getCreatedBy() { return createdBy; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

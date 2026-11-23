package br.com.ascend.the_system.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "calendar_events", indexes = @Index(name = "idx_calendar_events_user", columnList = "user_id"))
public class CalendarEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @JsonIgnore @Column(name = "user_id", nullable = false, updatable = false) public Long userId;
    @Column(nullable = false, length = 160) public String title;
    @Column(length = 2000) public String description;
    @Column(nullable = false, length = 10) public String date;
    @Column(length = 5) public String startTime;
    @Column(length = 5) public String endTime;
    @Column(length = 80) public String category;
    @Column(nullable = false, length = 10) public String recurrence = "none";
    public boolean completed;
    public boolean protectedEvent;
    public Integer reminderMinutes;
    @Column(length = 100) public String sourceKey;
    public boolean deleted;
}

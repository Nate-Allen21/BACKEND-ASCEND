package br.com.ascend.the_system.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "focus_sessions")
public class FocusSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "planned_minutes", nullable = false)
    private int plannedMinutes;

    @Column(name = "completed_minutes", nullable = false)
    private int completedMinutes;

    @Column(name = "dungeon_clear", nullable = false)
    private boolean dungeonClear;

    @Column(name = "started_at", nullable = false, updatable = false)
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    protected FocusSession() {
    }

    public FocusSession(User user, int plannedMinutes) {
        this.user = user;
        this.plannedMinutes = Math.max(plannedMinutes, 0);
        this.completedMinutes = 0;
        this.dungeonClear = false;
    }

    @PrePersist
    void prePersist() {
        startedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public int getPlannedMinutes() {
        return plannedMinutes;
    }

    public int getCompletedMinutes() {
        return completedMinutes;
    }

    public boolean isDungeonClear() {
        return dungeonClear;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public OffsetDateTime getFinishedAt() {
        return finishedAt;
    }

    public void finish(int completedMinutes, boolean dungeonClear) {
        this.completedMinutes = Math.max(completedMinutes, 0);
        this.dungeonClear = dungeonClear;
        this.finishedAt = OffsetDateTime.now();
    }
}

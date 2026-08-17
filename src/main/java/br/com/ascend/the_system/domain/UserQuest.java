package br.com.ascend.the_system.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "user_quests")
public class UserQuest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "system_mission_id", nullable = false)
    private SystemMission mission;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private QuestStatus status;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "granted_experience", nullable = false)
    private int grantedExperience;

    @Column(name = "granted_coins", nullable = false)
    private int grantedCoins;

    @Column(name = "debuff_applied", nullable = false)
    private boolean debuffApplied;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected UserQuest() {
    }

    public UserQuest(User user, SystemMission mission, LocalDate scheduledDate) {
        this.user = user;
        this.mission = mission;
        this.scheduledDate = scheduledDate;
        this.status = QuestStatus.PENDING;
        this.grantedExperience = 0;
        this.grantedCoins = 0;
        this.debuffApplied = false;
    }

    @PrePersist
    void prePersist() {
        createdAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public SystemMission getMission() {
        return mission;
    }

    public QuestStatus getStatus() {
        return status;
    }

    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public int getGrantedExperience() {
        return grantedExperience;
    }

    public int getGrantedCoins() {
        return grantedCoins;
    }

    public boolean isDebuffApplied() {
        return debuffApplied;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void complete(int experience, int coins) {
        if (status == QuestStatus.COMPLETED) {
            return;
        }

        this.status = QuestStatus.COMPLETED;
        this.grantedExperience = Math.max(experience, 0);
        this.grantedCoins = Math.max(coins, 0);
        this.completedAt = OffsetDateTime.now();
    }

    public void applyDebuff() {
        this.debuffApplied = true;
    }
}

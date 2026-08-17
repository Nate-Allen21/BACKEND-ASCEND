package br.com.ascend.the_system.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "users")
public class User {

    private static final int LEVEL_2_TOTAL_EXPERIENCE = 1_000;
    private static final int LEVEL_4_TOTAL_EXPERIENCE = 5_000;
    private static final int LEVEL_6_TOTAL_EXPERIENCE = 15_000;
    private static final int LEVEL_8_TOTAL_EXPERIENCE = 35_000;
    private static final int LEVEL_10_TOTAL_EXPERIENCE = 75_000;
    private static final int LEVEL_30_TOTAL_EXPERIENCE = 300_000;
    private static final int LEVEL_50_TOTAL_EXPERIENCE = 1_000_000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "player_rank", nullable = false, length = 4)
    private String rank;

    @Column(nullable = false)
    private int level;

    @Column(nullable = false)
    private int experience;

    @Column(nullable = false)
    private int nextLevelExperience;

    @Column(nullable = false)
    private int streakDays;

    @Column(nullable = false)
    private int coins;

    private LocalDate lastRoutineDate;
    private LocalDate lastPenaltyProcessedDate;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected User() {
    }

    public User(String name, String email, String passwordHash) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.rank = "E";
        this.level = 1;
        this.experience = 0;
        this.nextLevelExperience = nextLevelRequirementForLevel(this.level);
        this.streakDays = 0;
        this.coins = 0;
    }

    @PrePersist
    void prePersist() {
        createdAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getRank() {
        return rank;
    }

    public int getLevel() {
        return level;
    }

    public int getExperience() {
        return getAccumulatedExperienceBeforeCurrentLevel() + experience;
    }

    public int getNextLevelExperience() {
        return getAccumulatedExperienceBeforeCurrentLevel() + nextLevelExperience;
    }

    public int getStreakDays() {
        return streakDays;
    }

    public int getCoins() {
        return coins;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDate getLastPenaltyProcessedDate() {
        return lastPenaltyProcessedDate;
    }

    public int getDisplayedStreakDays(LocalDate today) {
        if (lastRoutineDate == null || lastRoutineDate.isBefore(today.minusDays(1))) {
            return 0;
        }

        return streakDays;
    }

    public void addRewards(int experienceReward, int coinReward) {
        this.experience += experienceReward;
        this.coins += coinReward;

        while (this.experience >= this.nextLevelExperience) {
            this.experience -= this.nextLevelExperience;
            this.level++;
            this.nextLevelExperience = nextLevelRequirementForLevel(this.level);
        }

        refreshRank();
    }

    public void applyPenalty(int experiencePenalty, int coinPenalty) {
        int remainingExperience = Math.max(getExperience() - Math.max(experiencePenalty, 0), 0);
        this.coins = Math.max(this.coins - Math.max(coinPenalty, 0), 0);

        this.level = 1;
        this.experience = remainingExperience;
        this.nextLevelExperience = nextLevelRequirementForLevel(this.level);

        while (this.experience >= this.nextLevelExperience) {
            this.experience -= this.nextLevelExperience;
            this.level++;
            this.nextLevelExperience = nextLevelRequirementForLevel(this.level);
        }

        refreshRank();
    }

    public void registerRoutineActivity(LocalDate routineDate) {
        if (lastRoutineDate == null) {
            streakDays = 1;
        } else if (lastRoutineDate.equals(routineDate.minusDays(1))) {
            streakDays += 1;
        } else if (!lastRoutineDate.equals(routineDate)) {
            streakDays = 1;
        }

        lastRoutineDate = routineDate;
    }

    public void markPenaltyProcessed(LocalDate penaltyDate) {
        this.lastPenaltyProcessedDate = penaltyDate;
    }

    private void refreshRank() {
        if (level >= 50) {
            rank = "SSS+";
        } else if (level >= 30) {
            rank = "S2";
        } else if (level >= 10) {
            rank = "S";
        } else if (level >= 8) {
            rank = "A";
        } else if (level >= 6) {
            rank = "B";
        } else if (level >= 4) {
            rank = "C";
        } else if (level >= 2) {
            rank = "D";
        } else {
            rank = "E";
        }
    }

    private int getAccumulatedExperienceBeforeCurrentLevel() {
        if (level <= 1) {
            return 0;
        }

        int accumulatedExperience = 0;
        for (int currentLevel = 1; currentLevel < level; currentLevel++) {
            accumulatedExperience += nextLevelRequirementForLevel(currentLevel);
        }

        return accumulatedExperience;
    }

    private int nextLevelRequirementForLevel(int level) {
        if (level <= 1) {
            return LEVEL_2_TOTAL_EXPERIENCE;
        }
        if (level <= 3) {
            return (LEVEL_4_TOTAL_EXPERIENCE - LEVEL_2_TOTAL_EXPERIENCE) / 2;
        }
        if (level <= 5) {
            return (LEVEL_6_TOTAL_EXPERIENCE - LEVEL_4_TOTAL_EXPERIENCE) / 2;
        }
        if (level <= 7) {
            return (LEVEL_8_TOTAL_EXPERIENCE - LEVEL_6_TOTAL_EXPERIENCE) / 2;
        }
        if (level <= 9) {
            return (LEVEL_10_TOTAL_EXPERIENCE - LEVEL_8_TOTAL_EXPERIENCE) / 2;
        }
        if (level <= 29) {
            return (LEVEL_30_TOTAL_EXPERIENCE - LEVEL_10_TOTAL_EXPERIENCE) / 20;
        }
        if (level <= 49) {
            return (LEVEL_50_TOTAL_EXPERIENCE - LEVEL_30_TOTAL_EXPERIENCE) / 20;
        }

        return (LEVEL_50_TOTAL_EXPERIENCE - LEVEL_30_TOTAL_EXPERIENCE) / 20;
    }
}

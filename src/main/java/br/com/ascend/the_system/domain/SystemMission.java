package br.com.ascend.the_system.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "system_missions")
public class SystemMission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String code;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PillarType pillar;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MissionKind kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AttributeType primaryAttribute;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private AttributeType secondaryAttribute;

    @Column(nullable = false)
    private int experienceReward;

    @Column(nullable = false)
    private int coinReward;

    @Column(nullable = false)
    private int statReward;

    @Column(nullable = false)
    private boolean requiredMission;

    @Column(nullable = false)
    private boolean penaltyQuest;

    @Column(nullable = false)
    private int focusMinutes;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected SystemMission() {
    }

    public SystemMission(
            String code,
            String title,
            String description,
            PillarType pillar,
            MissionKind kind,
            AttributeType primaryAttribute,
            AttributeType secondaryAttribute,
            int experienceReward,
            int coinReward,
            int statReward,
            boolean requiredMission,
            boolean penaltyQuest,
            int focusMinutes
    ) {
        this.code = code;
        this.title = title;
        this.description = description;
        this.pillar = pillar;
        this.kind = kind;
        this.primaryAttribute = primaryAttribute;
        this.secondaryAttribute = secondaryAttribute;
        this.experienceReward = experienceReward;
        this.coinReward = coinReward;
        this.statReward = statReward;
        this.requiredMission = requiredMission;
        this.penaltyQuest = penaltyQuest;
        this.focusMinutes = focusMinutes;
        this.active = true;
    }

    @PrePersist
    void prePersist() {
        createdAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public PillarType getPillar() {
        return pillar;
    }

    public MissionKind getKind() {
        return kind;
    }

    public AttributeType getPrimaryAttribute() {
        return primaryAttribute;
    }

    public AttributeType getSecondaryAttribute() {
        return secondaryAttribute;
    }

    public int getExperienceReward() {
        return experienceReward;
    }

    public int getCoinReward() {
        return coinReward;
    }

    public int getStatReward() {
        return statReward;
    }

    public boolean isRequiredMission() {
        return requiredMission;
    }

    public boolean isPenaltyQuest() {
        return penaltyQuest;
    }

    public int getFocusMinutes() {
        return focusMinutes;
    }

    public boolean isActive() {
        return active;
    }
}

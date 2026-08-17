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
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "mission_completions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_mission_completion_user_mission",
                columnNames = {"user_id", "mission_id"}
        )
)
public class MissionCompletion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "mission_id", nullable = false, length = 80)
    private String missionId;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime completedAt;

    protected MissionCompletion() {
    }

    public MissionCompletion(User user, String missionId) {
        this.user = user;
        this.missionId = missionId;
    }

    @PrePersist
    void prePersist() {
        completedAt = OffsetDateTime.now();
    }
}

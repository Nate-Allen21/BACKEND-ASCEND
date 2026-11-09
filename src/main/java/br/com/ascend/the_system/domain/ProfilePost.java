package br.com.ascend.the_system.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "profile_posts", indexes = @Index(name = "idx_profile_posts_user_created", columnList = "user_id, created_at"))
public class ProfilePost {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(nullable = false, length = 1200) private String content;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    protected ProfilePost() {}
    public ProfilePost(User user, String content) { this.user = user; this.content = content; }
    @PrePersist void prePersist() { createdAt = OffsetDateTime.now(); }
    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getContent() { return content; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}

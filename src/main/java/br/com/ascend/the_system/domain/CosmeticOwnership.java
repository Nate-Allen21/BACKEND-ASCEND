package br.com.ascend.the_system.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name="cosmetic_ownership", uniqueConstraints=@UniqueConstraint(name="uk_cosmetic_owner_item", columnNames={"user_id","item_id"}))
public class CosmeticOwnership {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="user_id", nullable=false) private User user;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="item_id", nullable=false) private StoreItem item;
    @Column(nullable=false, updatable=false) private OffsetDateTime acquiredAt;
    protected CosmeticOwnership(){}
    public CosmeticOwnership(User user, StoreItem item){this.user=user;this.item=item;}
    @PrePersist void prePersist(){acquiredAt=OffsetDateTime.now();}
    public Long getId(){return id;} public User getUser(){return user;} public StoreItem getItem(){return item;} public OffsetDateTime getAcquiredAt(){return acquiredAt;}
}

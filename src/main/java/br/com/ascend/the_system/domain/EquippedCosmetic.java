package br.com.ascend.the_system.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name="equipped_cosmetics", uniqueConstraints={@UniqueConstraint(name="uk_equipped_user_slot", columnNames={"user_id","slot"}), @UniqueConstraint(name="uk_equipped_owner_item", columnNames={"user_id","item_id"})})
public class EquippedCosmetic {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="user_id", nullable=false) private User user;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="item_id", nullable=false) private StoreItem item;
    @Column(nullable=false, length=32) private String slot;
    @Column(nullable=false) private OffsetDateTime equippedAt;
    protected EquippedCosmetic(){}
    public EquippedCosmetic(User user, StoreItem item, String slot){this.user=user;this.item=item;this.slot=slot;}
    @PrePersist void prePersist(){equippedAt=OffsetDateTime.now();}
    public Long getId(){return id;} public User getUser(){return user;} public StoreItem getItem(){return item;} public String getSlot(){return slot;}
}

package br.com.ascend.the_system.domain;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
@Entity
@Table(name="cosmetic_purchase_history")
public class CosmeticPurchase {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false) private User user;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="item_id",nullable=false) private StoreItem item;
 @Column(nullable=false) private int cost;
 @Column(nullable=false,updatable=false) private OffsetDateTime purchasedAt;
 protected CosmeticPurchase(){}
 public CosmeticPurchase(User user,StoreItem item,int cost){this.user=user;this.item=item;this.cost=cost;}
 @PrePersist void prePersist(){purchasedAt=OffsetDateTime.now();}
}

package br.com.ascend.the_system.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "shop_purchases", uniqueConstraints = {
    @UniqueConstraint(name="uk_shop_user_item_day", columnNames={"user_id", "item_id", "purchase_day"})
})
public class ShopPurchase {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="user_id", nullable=false)
    private User user;
    @Column(name="item_id", nullable=false, length=90)
    private String itemId;
    @Column(nullable=false, length=150)
    private String name;
    @Column(nullable=false)
    private int cost;
    @Column(name="purchase_day", nullable=false)
    private LocalDate day;
    @Column(nullable=false, updatable=false)
    private OffsetDateTime createdAt;
    protected ShopPurchase() {}
    public ShopPurchase(User user, String itemId, String name, int cost, LocalDate day) {
        this.user=user; this.itemId=itemId; this.name=name; this.cost=cost; this.day=day;
    }
    @PrePersist void beforeInsert() { createdAt=OffsetDateTime.now(); }
    public Long getId() { return id; }
    public String getItemId() { return itemId; }
    public String getName() { return name; }
    public int getCost() { return cost; }
    public LocalDate getDay() { return day; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}

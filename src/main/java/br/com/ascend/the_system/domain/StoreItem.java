package br.com.ascend.the_system.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "store_items")
public class StoreItem {
    @Id
    @Column(length = 90)
    private String id;
    @Column(nullable = false, length = 32)
    private String category;
    @Column(nullable = false, length = 32)
    private String rarity;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false, length = 500)
    private String description;
    @Column(nullable = false)
    private int cost;
    @Column(length = 500)
    private String previewUrl;
    @Column(nullable = false)
    private boolean active = true;
    @Column(nullable = false)
    private boolean featured;

    protected StoreItem() {}
    public StoreItem(String id, String category, String rarity, String name, String description, int cost, String previewUrl, boolean featured) {
        this.id=id; this.category=category; this.rarity=rarity; this.name=name; this.description=description;
        this.cost=cost; this.previewUrl=previewUrl; this.featured=featured; this.active=true;
    }
    public String getId(){return id;} public String getCategory(){return category;} public String getRarity(){return rarity;}
    public String getName(){return name;} public String getDescription(){return description;} public int getCost(){return cost;}
    public String getPreviewUrl(){return previewUrl;} public boolean isActive(){return active;} public boolean isFeatured(){return featured;}
}

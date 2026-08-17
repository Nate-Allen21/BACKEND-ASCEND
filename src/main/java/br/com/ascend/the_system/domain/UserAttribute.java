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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "user_attributes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_attribute_user_type",
                columnNames = {"user_id", "attribute_type"}
        )
)
public class UserAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "attribute_type", nullable = false, length = 32)
    private AttributeType attributeType;

    @Column(nullable = false)
    private int baseValue;

    @Column(nullable = false)
    private int bonusValue;

    protected UserAttribute() {
    }

    public UserAttribute(User user, AttributeType attributeType, int baseValue) {
        this.user = user;
        this.attributeType = attributeType;
        this.baseValue = baseValue;
        this.bonusValue = 0;
    }

    public AttributeType getAttributeType() {
        return attributeType;
    }

    public int getTotalValue() {
        return baseValue + bonusValue;
    }

    public void increase(int points) {
        this.bonusValue += Math.max(points, 0);
    }
}

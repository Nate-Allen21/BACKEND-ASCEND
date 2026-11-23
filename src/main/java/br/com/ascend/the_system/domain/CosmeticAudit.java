package br.com.ascend.the_system.domain;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity @Table(name="cosmetic_audit")
public class CosmeticAudit {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) private User actor;
    @Column(nullable=false) private String action;
    @Column(nullable=false) private String details;
    @Column(nullable=false) private int amount;
    @Column(nullable=false) private OffsetDateTime createdAt;
    protected CosmeticAudit() {}
    public CosmeticAudit(User actor,String action,String details,int amount){
        this.actor=actor; this.action=action; this.details=details; this.amount=amount; this.createdAt=OffsetDateTime.now();
    }
}

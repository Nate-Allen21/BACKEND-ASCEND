package br.com.ascend.the_system.domain;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
@Entity @Table(name="social_connections", uniqueConstraints=@UniqueConstraint(name="uk_social_connection",columnNames={"source_user_id","target_user_id","kind"}))
public class SocialConnection {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="source_user_id",nullable=false) private User source;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="target_user_id",nullable=false) private User target;
 @Column(nullable=false,length=12) private String kind;
 @Column(nullable=false,length=12) private String status;
 @Column(name="created_at",nullable=false,updatable=false) private OffsetDateTime createdAt;
 protected SocialConnection(){}
 public SocialConnection(User source,User target,String kind,String status){this.source=source;this.target=target;this.kind=kind;this.status=status;}
 @PrePersist void pre(){createdAt=OffsetDateTime.now();}
 public Long getId(){return id;} public User getSource(){return source;} public User getTarget(){return target;} public String getKind(){return kind;} public String getStatus(){return status;} public void setStatus(String v){status=v;}
}

package br.com.ascend.the_system.domain;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
@Entity @Table(name="connect_conversations", uniqueConstraints=@UniqueConstraint(columnNames={"first_user_id","second_user_id"}))
public class ConnectConversation {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=true) @JoinColumn(name="first_user_id",nullable=true) public User first;
 @ManyToOne(fetch=FetchType.LAZY,optional=true) @JoinColumn(name="second_user_id",nullable=true) public User second;
 @Column(nullable=false) public OffsetDateTime updatedAt;
 public long firstRead=0; public long secondRead=0;
 protected ConnectConversation() {}
 public ConnectConversation(User a,User b){first=a;second=b;updatedAt=OffsetDateTime.now();}
 public User other(User u){return first != null && first.getId().equals(u.getId())?second:first;}
}

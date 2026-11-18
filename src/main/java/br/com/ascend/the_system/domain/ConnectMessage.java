package br.com.ascend.the_system.domain;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
@Entity @Table(name="connect_messages",uniqueConstraints=@UniqueConstraint(columnNames={"sender_id","client_id"}),indexes={@Index(columnList="conversation_id,id"),@Index(columnList="sender_id,createdAt")})
public class ConnectMessage {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="conversation_id",nullable=false) public ConnectConversation conversation;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="sender_id",nullable=false) public User sender;
 @Column(name="client_id",nullable=false,length=36) public String clientId;
 @Column(nullable=false,length=2000) public String content;
 @Column(nullable=false,updatable=false) public OffsetDateTime createdAt;
 protected ConnectMessage(){}
 public ConnectMessage(ConnectConversation c,User s,String clientId,String content){this.conversation=c;this.sender=s;this.clientId=clientId;this.content=content;this.createdAt=OffsetDateTime.now();}
}

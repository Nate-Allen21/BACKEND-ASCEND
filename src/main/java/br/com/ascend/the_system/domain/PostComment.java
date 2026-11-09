package br.com.ascend.the_system.domain;
import jakarta.persistence.*; import java.time.OffsetDateTime;
@Entity @Table(name="profile_post_comments",indexes=@Index(name="idx_post_comments_post_created",columnList="post_id, created_at"))
public class PostComment {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="post_id",nullable=false) private ProfilePost post;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false) private User user;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="parent_id") private PostComment parent;
 @Column(nullable=false,length=600) private String content;
 @Column(name="created_at",nullable=false,updatable=false) private OffsetDateTime createdAt;
 protected PostComment(){} public PostComment(ProfilePost p,User u,PostComment parent,String c){post=p;user=u;this.parent=parent;content=c;}
 @PrePersist void pre(){createdAt=OffsetDateTime.now();} public Long getId(){return id;} public ProfilePost getPost(){return post;} public User getUser(){return user;} public PostComment getParent(){return parent;} public String getContent(){return content;} public OffsetDateTime getCreatedAt(){return createdAt;}
}

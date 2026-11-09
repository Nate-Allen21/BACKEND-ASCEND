package br.com.ascend.the_system.domain;
import jakarta.persistence.*;
@Entity @Table(name="profile_post_likes",uniqueConstraints=@UniqueConstraint(name="uk_post_like",columnNames={"post_id","user_id"}))
public class PostLike {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="post_id",nullable=false) private ProfilePost post;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false) private User user;
 protected PostLike(){} public PostLike(ProfilePost p,User u){post=p;user=u;} public Long getId(){return id;}
}

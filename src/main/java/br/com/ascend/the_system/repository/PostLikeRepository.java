package br.com.ascend.the_system.repository;
import br.com.ascend.the_system.domain.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface PostLikeRepository extends JpaRepository<PostLike,Long> { long countByPost(ProfilePost post); boolean existsByPostAndUser(ProfilePost post,User user); java.util.Optional<PostLike> findByPostAndUser(ProfilePost post,User user); void deleteByPost(ProfilePost post); }

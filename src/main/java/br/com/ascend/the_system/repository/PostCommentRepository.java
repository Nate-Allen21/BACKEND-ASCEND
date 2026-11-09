package br.com.ascend.the_system.repository;
import br.com.ascend.the_system.domain.*; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface PostCommentRepository extends JpaRepository<PostComment,Long> { List<PostComment> findTop100ByPostOrderByCreatedAtAsc(ProfilePost post); void deleteByPostAndParentIsNotNull(ProfilePost post); void deleteByPostAndParentIsNull(ProfilePost post); }

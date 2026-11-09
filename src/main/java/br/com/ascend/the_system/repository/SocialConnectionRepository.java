package br.com.ascend.the_system.repository;
import br.com.ascend.the_system.domain.*; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface SocialConnectionRepository extends JpaRepository<SocialConnection,Long> {
 Optional<SocialConnection> findBySourceAndTargetAndKind(User source,User target,String kind);
 boolean existsBySourceAndTargetAndKindAndStatus(User source,User target,String kind,String status);
 boolean existsBySourceAndTargetAndKind(User source,User target,String kind);
 List<SocialConnection> findBySourceAndKindAndStatus(User source,String kind,String status);
 List<SocialConnection> findByTargetAndKindAndStatus(User target,String kind,String status);
}

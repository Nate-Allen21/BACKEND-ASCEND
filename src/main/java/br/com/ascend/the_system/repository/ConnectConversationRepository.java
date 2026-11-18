package br.com.ascend.the_system.repository;
import br.com.ascend.the_system.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface ConnectConversationRepository extends JpaRepository<ConnectConversation,Long> {
 Optional<ConnectConversation> findByFirstAndSecond(User first,User second);
 @Query("select c from ConnectConversation c where c.first = :user or c.second = :user order by c.updatedAt desc, c.id desc") List<ConnectConversation> forUser(@Param("user") User user);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select c from ConnectConversation c where c.id = :id") Optional<ConnectConversation> lockById(@Param("id") Long id);
}

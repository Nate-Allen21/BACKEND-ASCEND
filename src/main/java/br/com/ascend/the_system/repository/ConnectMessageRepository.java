package br.com.ascend.the_system.repository;
import br.com.ascend.the_system.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.Pageable;
import java.time.OffsetDateTime;
import java.util.*;
public interface ConnectMessageRepository extends JpaRepository<ConnectMessage,Long> {
 Optional<ConnectMessage> findBySenderAndClientId(User sender,String clientId);
 List<ConnectMessage> findByConversationAndIdLessThanOrderByIdDesc(ConnectConversation c,Long before,Pageable page);
 Optional<ConnectMessage> findFirstByConversationOrderByIdDesc(ConnectConversation c);
 long countByConversationAndSenderNotAndIdGreaterThan(ConnectConversation c,User sender,Long id);
 long countBySenderAndCreatedAtAfter(User sender,OffsetDateTime after);
}

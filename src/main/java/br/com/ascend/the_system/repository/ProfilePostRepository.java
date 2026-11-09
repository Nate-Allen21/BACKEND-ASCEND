package br.com.ascend.the_system.repository;
import br.com.ascend.the_system.domain.ProfilePost;
import br.com.ascend.the_system.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ProfilePostRepository extends JpaRepository<ProfilePost, Long> {
    List<ProfilePost> findTop50ByUserOrderByCreatedAtDesc(User user);
}

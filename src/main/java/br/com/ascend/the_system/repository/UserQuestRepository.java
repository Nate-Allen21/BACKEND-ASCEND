package br.com.ascend.the_system.repository;

import br.com.ascend.the_system.domain.UserQuest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserQuestRepository extends JpaRepository<UserQuest, Long> {

    Optional<UserQuest> findByIdAndUser_Id(Long id, Long userId);

    List<UserQuest> findTop10ByUser_IdOrderByScheduledDateDesc(Long userId);
}

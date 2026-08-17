package br.com.ascend.the_system.repository;

import br.com.ascend.the_system.domain.MissionCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface MissionCompletionRepository extends JpaRepository<MissionCompletion, Long> {

    boolean existsByUser_IdAndMissionId(Long userId, String missionId);

    long countByUser_Id(Long userId);

    long countByUser_IdAndMissionIdStartingWith(Long userId, String missionIdPrefix);

    @Query("""
            select completion.missionId
            from MissionCompletion completion
            where completion.user.id = :userId
              and completion.missionId in :missionIds
            """)
    List<String> findCompletedMissionIds(
            @Param("userId") Long userId,
            @Param("missionIds") Collection<String> missionIds
    );
}

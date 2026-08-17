package br.com.ascend.the_system.repository;

import br.com.ascend.the_system.domain.SystemMission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemMissionRepository extends JpaRepository<SystemMission, Long> {
}

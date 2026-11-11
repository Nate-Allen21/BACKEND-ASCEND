package br.com.ascend.the_system.repository;
import br.com.ascend.the_system.domain.EquippedCosmetic;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface EquippedCosmeticRepository extends JpaRepository<EquippedCosmetic,Long> {
 List<EquippedCosmetic> findByUser_Id(Long userId);
 Optional<EquippedCosmetic> findByUser_IdAndSlot(Long userId,String slot);
 void deleteByUser_IdAndSlot(Long userId,String slot);
}

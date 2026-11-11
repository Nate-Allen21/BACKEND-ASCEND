package br.com.ascend.the_system.repository;
import br.com.ascend.the_system.domain.CosmeticOwnership;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface CosmeticOwnershipRepository extends JpaRepository<CosmeticOwnership,Long> {
 boolean existsByUser_IdAndItem_Id(Long userId,String itemId);
 List<CosmeticOwnership> findByUser_IdOrderByAcquiredAtDesc(Long userId);
 boolean existsByUser_IdAndItem_IdAndItem_Category(Long userId,String itemId,String category);
}

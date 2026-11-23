package br.com.ascend.the_system.repository;
import br.com.ascend.the_system.domain.EquippedCosmetic;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface EquippedCosmeticRepository extends JpaRepository<EquippedCosmetic,Long> {
 @org.springframework.data.jpa.repository.Query("""
     select new br.com.ascend.the_system.dto.FrameArtwork(e.user.id,i.staticAsset,i.animatedAsset)
       from EquippedCosmetic e join e.item i
       where e.slot='AVATAR_FRAME' and i.staticAsset is not null and e.user.id in :ids
     """)
 List<br.com.ascend.the_system.dto.FrameArtwork> framesFor(@org.springframework.data.repository.query.Param("ids") java.util.Collection<Long> ids);
 List<EquippedCosmetic> findByUser_Id(Long userId);
 Optional<EquippedCosmetic> findByUser_IdAndSlot(Long userId,String slot);
 void deleteByUser_IdAndSlot(Long userId,String slot);
}

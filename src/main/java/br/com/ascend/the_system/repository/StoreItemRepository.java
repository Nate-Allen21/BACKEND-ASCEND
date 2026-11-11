package br.com.ascend.the_system.repository;
import br.com.ascend.the_system.domain.StoreItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface StoreItemRepository extends JpaRepository<StoreItem,String> { List<StoreItem> findByActiveTrueOrderByFeaturedDescIdAsc(); }

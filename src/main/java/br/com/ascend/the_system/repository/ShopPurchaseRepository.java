package br.com.ascend.the_system.repository;

import br.com.ascend.the_system.domain.ShopPurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface ShopPurchaseRepository extends JpaRepository<ShopPurchase, Long> {
    boolean existsByUser_IdAndItemIdAndDay(Long userId, String itemId, LocalDate day);
    List<ShopPurchase> findTop200ByUser_IdOrderByCreatedAtDesc(Long userId);
}

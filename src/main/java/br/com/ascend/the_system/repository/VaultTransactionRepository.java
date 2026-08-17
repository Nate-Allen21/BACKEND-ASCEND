package br.com.ascend.the_system.repository;

import br.com.ascend.the_system.domain.VaultTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VaultTransactionRepository extends JpaRepository<VaultTransaction, Long> {

    List<VaultTransaction> findTop10ByUser_IdOrderByCreatedAtDesc(Long userId);
}

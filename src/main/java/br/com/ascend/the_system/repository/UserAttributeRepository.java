package br.com.ascend.the_system.repository;

import br.com.ascend.the_system.domain.AttributeType;
import br.com.ascend.the_system.domain.UserAttribute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAttributeRepository extends JpaRepository<UserAttribute, Long> {

    Optional<UserAttribute> findByUser_IdAndAttributeType(Long userId, AttributeType attributeType);

    List<UserAttribute> findAllByUser_Id(Long userId);
}

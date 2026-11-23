package br.com.ascend.the_system.repository;
import br.com.ascend.the_system.domain.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {
    List<CalendarEvent> findAllByUserId(Long userId);
    Optional<CalendarEvent> findByIdAndUserId(Long id, Long userId);
}

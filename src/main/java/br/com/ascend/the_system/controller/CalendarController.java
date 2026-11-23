package br.com.ascend.the_system.controller;

import br.com.ascend.the_system.domain.CalendarEvent;
import br.com.ascend.the_system.exception.BadRequestException;
import br.com.ascend.the_system.repository.CalendarEventRepository;
import br.com.ascend.the_system.service.AuthService;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/calendar/events")
public class CalendarController {
    private final AuthService auth;
    private final CalendarEventRepository events;
    public CalendarController(AuthService auth, CalendarEventRepository events) { this.auth = auth; this.events = events; }
    @GetMapping public List<CalendarEvent> list(@RequestHeader(value="Authorization", required=false) String authorization) {
        return events.findAllByUserId(auth.resolveUser(authorization).getId());
    }
    @PostMapping public CalendarEvent create(@RequestHeader(value="Authorization", required=false) String authorization, @RequestBody CalendarEvent input) {
        Long userId = auth.resolveUser(authorization).getId();
        CalendarEvent event = new CalendarEvent(); event.userId = userId;
        copy(input, event); return events.save(event);
    }
    @PutMapping("/{id}") public CalendarEvent update(@RequestHeader(value="Authorization", required=false) String authorization, @PathVariable Long id, @RequestBody CalendarEvent input) {
        CalendarEvent event = owned(authorization, id); copy(input, event); return events.save(event);
    }
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}") public void delete(@RequestHeader(value="Authorization", required=false) String authorization, @PathVariable Long id) {
        CalendarEvent event = owned(authorization, id);
        if (event.sourceKey != null && !event.sourceKey.isBlank()) { event.deleted = true; events.save(event); }
        else events.delete(event);
    }
    private CalendarEvent owned(String authorization, Long id) {
        return events.findByIdAndUserId(id, auth.resolveUser(authorization).getId()).orElseThrow(() -> new BadRequestException("Evento não encontrado."));
    }
    private void copy(CalendarEvent in, CalendarEvent out) {
        if (in.title == null || in.title.isBlank() || in.title.length() > 160 || in.description != null && in.description.length() > 2000 || in.category != null && in.category.length() > 80 || in.sourceKey != null && in.sourceKey.length() > 100) throw new BadRequestException("Dados do evento inválidos.");
        try {
            if (in.date == null || !in.date.matches("\\d{4}-\\d{2}-\\d{2}") ||
                    in.startTime != null && !in.startTime.isBlank() && !in.startTime.matches("\\d{2}:\\d{2}") ||
                    in.endTime != null && !in.endTime.isBlank() && !in.endTime.matches("\\d{2}:\\d{2}")) {
                throw new BadRequestException("Data ou horário inválido.");
            }
            LocalDate.parse(in.date);
            if (in.startTime != null && !in.startTime.isBlank()) LocalTime.parse(in.startTime);
            if (in.endTime != null && !in.endTime.isBlank()) LocalTime.parse(in.endTime);
            if (in.endTime != null && !in.endTime.isBlank() && (in.startTime == null || in.startTime.isBlank() || !LocalTime.parse(in.endTime).isAfter(LocalTime.parse(in.startTime)))) throw new BadRequestException("O horário final deve ser posterior ao inicial.");
        } catch (DateTimeParseException | NullPointerException e) { throw new BadRequestException("Data ou horário inválido."); }
        if (!Set.of("none", "daily", "weekly", "monthly").contains(in.recurrence == null ? "none" : in.recurrence) || in.reminderMinutes != null && (in.reminderMinutes < 0 || in.reminderMinutes > 10080)) throw new BadRequestException("Recorrência ou lembrete inválido.");
        if (in.reminderMinutes != null && (in.startTime == null || in.startTime.isBlank())) throw new BadRequestException("Informe o horário inicial para configurar um lembrete.");
        out.title=in.title.trim(); out.description=in.description; out.date=in.date; out.startTime=in.startTime; out.endTime=in.endTime; out.category=in.category;
        out.recurrence=in.recurrence == null ? "none" : in.recurrence; out.completed=in.completed; out.protectedEvent=in.protectedEvent; out.reminderMinutes=in.reminderMinutes;
        if (out.id == null) out.sourceKey=in.sourceKey;
        out.deleted=false;
    }
}

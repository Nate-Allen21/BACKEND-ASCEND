package br.com.ascend.the_system.service;

import br.com.ascend.the_system.domain.User;
import br.com.ascend.the_system.dto.AccountDeletionRequest;
import br.com.ascend.the_system.exception.BadRequestException;
import br.com.ascend.the_system.exception.ConflictException;
import br.com.ascend.the_system.exception.UnauthorizedException;
import br.com.ascend.the_system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AccountDeletionService {
    private static final String FINAL_CONFIRMATION = "EXCLUIR MINHA CONTA";
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long ATTEMPT_WINDOW_SECONDS = 15 * 60;
    private static final ConcurrentHashMap<Long, AttemptWindow> FAILED_ATTEMPTS = new ConcurrentHashMap<>();

    private final AuthService auth;
    private final PasswordService passwords;
    private final JdbcTemplate jdbc;
    private final UserRepository users;

    @Value("${ASCEND_COSMETIC_ADMIN_IDS:}")
    private String configuredAdminIds;

    public AccountDeletionService(AuthService auth, PasswordService passwords, JdbcTemplate jdbc, UserRepository users) {
        this.auth = auth;
        this.passwords = passwords;
        this.jdbc = jdbc;
        this.users = users;
    }

    @Transactional
    public void delete(String authorization, AccountDeletionRequest request) {
        User authenticatedUser = auth.resolveUser(authorization);
        User user = users.findByIdForUpdate(authenticatedUser.getId())
                .orElseThrow(() -> new UnauthorizedException("Sessão inválida ou expirada."));
        validateRequest(request);
        checkAttemptLimit(user.getId());

        boolean passwordVerified = request.password() != null && passwords.matches(request.password(), user.getPasswordHash());
        boolean googleVerified = auth.verifiesRecentGoogleCredential(user, request.googleCredential());
        if (!passwordVerified && !googleVerified) {
            registerFailure(user.getId());
            if (request.googleCredential() != null && !request.googleCredential().isBlank()) {
                throw new UnauthorizedException("Não foi possível validar a conta Google. Reautentique com a mesma conta vinculada e tente novamente.");
            }
            if (request.password() != null && !request.password().isBlank()) {
                throw new UnauthorizedException("Senha incorreta. Verifique-a e tente novamente.");
            }
            throw new UnauthorizedException("Informe sua senha ou reautentique com Google.");
        }
        FAILED_ATTEMPTS.remove(user.getId());

        if (isConfiguredAdministrator(user.getId())) {
            throw new ConflictException("Transfira a administração da plataforma antes de excluir esta conta.");
        }

        deleteOwnedData(user.getId());
        users.delete(user);
        users.flush();
    }

    private void validateRequest(AccountDeletionRequest request) {
        if (request == null || !FINAL_CONFIRMATION.equals(request.confirmation()) || !request.acknowledged()) {
            throw new BadRequestException("Confirme a frase e reconheça que a exclusão é permanente.");
        }
        if (request.reason() != null && request.reason().length() > 500) {
            throw new BadRequestException("O motivo informado é muito longo.");
        }
    }

    private boolean isConfiguredAdministrator(Long userId) {
        if (configuredAdminIds == null || configuredAdminIds.isBlank()) return false;
        return List.of(configuredAdminIds.split(",")).stream()
                .map(String::trim)
                .anyMatch(id -> id.equals(String.valueOf(userId)));
    }

    private void checkAttemptLimit(Long userId) {
        AttemptWindow window = FAILED_ATTEMPTS.get(userId);
        if (window == null) return;
        long now = Instant.now().getEpochSecond();
        if (now - window.startedAt >= ATTEMPT_WINDOW_SECONDS) {
            FAILED_ATTEMPTS.remove(userId, window);
            return;
        }
        if (window.failures >= MAX_FAILED_ATTEMPTS) {
            throw new ConflictException("Muitas tentativas. Aguarde 15 minutos antes de tentar novamente.");
        }
    }

    private void registerFailure(Long userId) {
        long now = Instant.now().getEpochSecond();
        FAILED_ATTEMPTS.compute(userId, (ignored, window) -> window == null || now - window.startedAt >= ATTEMPT_WINDOW_SECONDS
                ? new AttemptWindow(now, 1)
                : new AttemptWindow(window.startedAt, window.failures + 1));
    }

    private void deleteOwnedData(Long userId) {
        // Delete message bodies authored by this account. Keep other participants' messages.
        jdbc.update("delete from connect_messages where sender_id = ?", userId);
        // The migration makes participant references nullable so the remaining conversation can survive.
        jdbc.update("update connect_conversations set first_user_id = null where first_user_id = ?", userId);
        jdbc.update("update connect_conversations set second_user_id = null where second_user_id = ?", userId);

        jdbc.update("delete from profile_post_likes where user_id = ? or post_id in (select id from profile_posts where user_id = ?)", userId, userId);
        jdbc.update("delete from profile_post_comments where parent_id in (select id from profile_post_comments where user_id = ?)", userId);
        jdbc.update("delete from profile_post_comments where post_id in (select id from profile_posts where user_id = ?) and parent_id is not null", userId);
        jdbc.update("delete from profile_post_comments where user_id = ? or post_id in (select id from profile_posts where user_id = ?)", userId, userId);
        jdbc.update("delete from profile_posts where user_id = ?", userId);
        jdbc.update("delete from social_connections where source_user_id = ? or target_user_id = ?", userId, userId);
        jdbc.update("delete from cosmetic_audit where actor_id = ? or (action = 'TEST_BALANCE' and details like ?)", userId, "#" + userId + " %");
        jdbc.update("delete from calendar_events where user_id = ?", userId);
        jdbc.update("delete from equipped_cosmetics where user_id = ?", userId);
        jdbc.update("delete from cosmetic_ownership where user_id = ?", userId);
        jdbc.update("delete from cosmetic_purchase_history where user_id = ?", userId);
        jdbc.update("delete from shop_purchases where user_id = ?", userId);
        jdbc.update("delete from vault_transactions where user_id = ? or description like ?", userId, "Teste por #" + userId + ":%");
        jdbc.update("delete from focus_sessions where user_id = ?", userId);
        jdbc.update("delete from mission_completions where user_id = ?", userId);
        jdbc.update("delete from user_quests where user_id = ?", userId);
        jdbc.update("delete from user_attributes where user_id = ?", userId);
        jdbc.update("delete from user_sessions where user_id = ?", userId);
    }

    private record AttemptWindow(long startedAt, int failures) {}
}

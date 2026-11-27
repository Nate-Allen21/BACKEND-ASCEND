package br.com.ascend.the_system.service;

import br.com.ascend.the_system.domain.User;
import br.com.ascend.the_system.exception.BadRequestException;
import br.com.ascend.the_system.repository.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RecoveryStore {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ACTIVE = " used_at is null and revoked_at is null ";
    private final JdbcTemplate jdbc;
    private final UserRepository users;
    private final PasswordService passwords;

    RecoveryStore(JdbcTemplate jdbc, UserRepository users, PasswordService passwords) {
        this.jdbc = jdbc;
        this.users = users;
        this.passwords = passwords;
    }

    @Transactional
    public void request(String email) {
        users.findByEmailForUpdate(email).ifPresent(user -> {
            revoke(user.getId());
            jdbc.update("""
                    insert into ascend_recovery.deliveries(user_id) values (?)
                    on conflict (user_id) do update set requested_at = excluded.requested_at,
                    next_attempt = excluded.next_attempt, attempts = 0, delivery_id = null
                    """, user.getId());
        });
    }

    public List<Long> due() {
        return jdbc.queryForList("""
                select user_id from ascend_recovery.deliveries where next_attempt <= current_timestamp
                and attempts < 3 and requested_at > current_timestamp - interval '15 minutes'
                order by next_attempt limit 8
                """, Long.class);
    }

    // Every issuer, consumer and session creator locks the user first.
    @Transactional
    public Optional<Delivery> claim(long userId) {
        User user = users.findByIdForUpdate(userId).orElse(null);
        if (user == null) return Optional.empty();
        UUID lease = UUID.randomUUID();
        int claimed = jdbc.update("""
                update ascend_recovery.deliveries set attempts = attempts + 1, delivery_id = ?,
                next_attempt = clock_timestamp() + interval '30 seconds'
                where user_id = ? and next_attempt <= clock_timestamp() and attempts < 3
                and requested_at > clock_timestamp() - interval '15 minutes'
                """, lease, userId);
        if (claimed == 0) return Optional.empty();
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        revoke(userId);
        jdbc.update("""
                insert into ascend_recovery.tokens(user_id, token_hash, created_at, expires_at)
                values (?, ?, statement_timestamp(), statement_timestamp() + interval '15 minutes')
                """, userId, RecoveryLimits.digest(token));
        return Optional.of(new Delivery(userId, lease, user.getEmail(), token));
    }

    @Transactional
    public void finish(Delivery delivery, boolean sent) {
        if (sent) {
            jdbc.update("delete from ascend_recovery.deliveries where user_id = ? and delivery_id = ?",
                    delivery.userId(), delivery.lease());
        } else {
            jdbc.update("update ascend_recovery.tokens set revoked_at = clock_timestamp() where token_hash = ? and "
                    + ACTIVE, RecoveryLimits.digest(delivery.token()));
        }
    }

    public Optional<Long> owner(String hash) {
        return jdbc.queryForList("select user_id from ascend_recovery.tokens where token_hash = ? and "
                + ACTIVE + " and expires_at > clock_timestamp()", Long.class, hash).stream().findFirst();
    }

    @Transactional
    public void reset(long userId, String hash, String newPassword) {
        User user = users.findByIdForUpdate(userId).orElseThrow(RecoveryStore::invalidLink);
        int changed = jdbc.update("update ascend_recovery.tokens set used_at = clock_timestamp() "
                + "where user_id = ? and token_hash = ? and " + ACTIVE + " and expires_at > clock_timestamp()",
                userId, hash);
        if (changed != 1) throw invalidLink();
        user.updatePasswordHash(passwords.hashForRecovery(newPassword));
        jdbc.update("delete from user_sessions where user_id = ?", userId);
        jdbc.update("delete from ascend_recovery.deliveries where user_id = ?", userId);
    }

    private void revoke(long userId) {
        jdbc.update("update ascend_recovery.tokens set revoked_at = clock_timestamp() where user_id = ? and "
                + ACTIVE, userId);
    }

    public void prune() {
        jdbc.update("delete from ascend_recovery.limits where until_at < current_timestamp");
        jdbc.update("delete from ascend_recovery.tokens where expires_at < current_timestamp - interval '1 day'");
        jdbc.update("delete from ascend_recovery.deliveries where requested_at < current_timestamp - interval '15 minutes'");
    }

    public static BadRequestException invalidLink() {
        return new BadRequestException("Este link é inválido ou expirou. Solicite um novo link.");
    }

    public record Delivery(long userId, UUID lease, String email, String token) {
        @Override public String toString() { return "RecoveryDelivery"; }
    }
}

package br.com.ascend.the_system.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class RecoveryLimits {
    private final JdbcTemplate jdbc;

    RecoveryLimits(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Called outside the password transaction so a rejected attempt still counts.
    public boolean allow(String scope, String key, int maximum) {
        return jdbc.update("""
                insert into ascend_recovery.limits as l (bucket, until_at, attempts)
                values (?, current_timestamp + interval '15 minutes', 1)
                on conflict (bucket) do update set
                    attempts = case when l.until_at <= current_timestamp then 1 else l.attempts + 1 end,
                    until_at = case when l.until_at <= current_timestamp then excluded.until_at else l.until_at end
                where l.until_at <= current_timestamp or l.attempts < ?
                """, digest(scope + ":" + key), maximum) == 1;
    }

    public static String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

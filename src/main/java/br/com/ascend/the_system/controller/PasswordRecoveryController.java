package br.com.ascend.the_system.controller;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import com.google.common.net.InetAddresses;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import br.com.ascend.the_system.exception.BadRequestException;
import br.com.ascend.the_system.service.RecoveryLimits;
import br.com.ascend.the_system.service.RecoveryMailer;
import br.com.ascend.the_system.service.RecoveryStore;

@RestController
@RequestMapping("/api/auth")
public class PasswordRecoveryController {
    private static final String GENERIC = "Se existir uma conta associada a este e-mail, você receberá as instruções para redefinir sua senha.";
    private static final Pattern EMAIL = Pattern.compile("[^\\s@,;<>\\p{Cntrl}]+@[^\\s@,;<>\\p{Cntrl}]+\\.[^\\s@,;<>\\p{Cntrl}]+");
    private static final Pattern TOKEN = Pattern.compile("[A-Za-z0-9_-]{43}");
    private final RecoveryLimits limits;
    private final RecoveryStore store;
    private final RecoveryMailer mailer;
    private final boolean trustEdge;

    PasswordRecoveryController(RecoveryLimits limits, RecoveryStore store, RecoveryMailer mailer,
                               @Value("${ASCEND_RECOVERY_TRUST_RENDER_EDGE:false}") boolean trustEdge) {
        this.limits = limits;
        this.store = store;
        this.mailer = mailer;
        this.trustEdge = trustEdge;
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgot(@RequestBody EmailRequest body, HttpServletRequest request) {
        if (!mailer.configured()) return unavailable();
        if (!limits.allow("forgot-ip", address(request), 20)) return tooMany();
        String email = body.email() == null ? "" : body.email().strip().toLowerCase(Locale.ROOT);
        if (email.length() > 160 || !EMAIL.matcher(email).matches()) {
            throw new BadRequestException("Informe um e-mail válido.");
        }
        if (limits.allow("forgot-email", email, 3)) store.request(email);
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).body(Map.of("message", GENERIC));
    }

    @PostMapping("/reset-password/validate")
    public ResponseEntity<?> validate(@RequestBody TokenRequest body, HttpServletRequest request) {
        if (!limits.allow("reset-ip", address(request), 30)) return tooMany();
        String hash = tokenHash(body.token());
        store.owner(hash).orElseThrow(RecoveryStore::invalidLink);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    private String tokenHash(String token) {
        if (token == null || !TOKEN.matcher(token).matches()) throw RecoveryStore.invalidLink();
        return RecoveryLimits.digest(token);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> reset(@RequestBody ResetRequest body, HttpServletRequest request) {
        if (!limits.allow("reset-ip", address(request), 30)) return tooMany();
        String hash = tokenHash(body.token());
        long user = store.owner(hash).orElseThrow(RecoveryStore::invalidLink);
        if (!limits.allow("reset-account", Long.toString(user), 8)) return tooMany();
        String password = body.newPassword();
        if (password == null || password.isBlank() || password.length() < 8 || password.length() > 128) {
            throw new BadRequestException("A senha deve ter entre 8 e 128 caracteres.");
        }
        if (!password.equals(body.confirmPassword())) throw new BadRequestException("As senhas devem ser iguais.");
        store.reset(user, hash, password);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    private String address(HttpServletRequest request) {
        String edge = trustEdge ? request.getHeader("CF-Connecting-IP") : null;
        String ip = edge != null && InetAddresses.isInetAddress(edge) ? edge : request.getRemoteAddr();
        return InetAddresses.isInetAddress(ip) ? InetAddresses.toAddrString(InetAddresses.forString(ip)) : "unknown";
    }

    private ResponseEntity<?> unavailable() {
        return ResponseEntity.status(503).cacheControl(CacheControl.noStore())
                .body(Map.of("message", "A recuperação está temporariamente indisponível. Tente novamente mais tarde."));
    }

    private ResponseEntity<?> tooMany() {
        return ResponseEntity.status(429).header("Retry-After", "900").cacheControl(CacheControl.noStore())
                .body(Map.of("message", "Muitas tentativas. Tente novamente em 15 minutos."));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> malformedBody() {
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore())
                .body(Map.of("message", "Informe os campos de recuperação válidos."));
    }

    public record EmailRequest(String email) {
        @Override public String toString() { return "RecoveryEmailRequest"; }
    }

    public record TokenRequest(String token) {
        @Override public String toString() { return "RecoveryTokenRequest"; }
    }

    public record ResetRequest(String token, String newPassword, String confirmPassword) {
        @Override public String toString() { return "PasswordResetRequest"; }
    }
}

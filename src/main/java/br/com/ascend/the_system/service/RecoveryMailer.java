package br.com.ascend.the_system.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class RecoveryMailer {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
    private final ObjectMapper json;
    private final String key;
    private final String from;
    private final String origin;
    private final String template;
    private final URI endpoint;

    @Autowired
    RecoveryMailer(ObjectMapper json, @Value("${RESEND_API_KEY:}") String key,
                   @Value("${ASCEND_RECOVERY_FROM:}") String from,
                   @Value("${ASCEND_RECOVERY_ORIGIN:}") String configuredOrigin) throws IOException {
        this(json, key, from, configuredOrigin, URI.create("https://api.resend.com/emails"));
    }

    RecoveryMailer(ObjectMapper json, String key, String from, String configuredOrigin, URI endpoint) throws IOException {
        this.json = json;
        this.endpoint = endpoint;
        this.key = key.trim();
        this.from = from.trim();
        this.origin = httpsOrigin(configuredOrigin);
        this.template = new ClassPathResource("mail/password-recovery.html").getContentAsString(StandardCharsets.UTF_8);
    }

    private static String httpsOrigin(String value) {
        if (value.isBlank()) return "";
        try {
            URI uri = URI.create(value.trim());
            if ("https".equals(uri.getScheme()) && uri.getHost() != null && uri.getRawUserInfo() == null
                    && uri.getRawQuery() == null && uri.getRawFragment() == null
                    && (uri.getRawPath().isEmpty() || "/".equals(uri.getRawPath()))) {
                return "https://" + uri.getRawAuthority();
            }
        } catch (IllegalArgumentException ignored) {
            // Misconfiguration affects only recovery, so existing logins continue to work.
        }
        return "";
    }

    public boolean configured() {
        return !key.isBlank() && !from.isBlank() && !origin.isEmpty();
    }

    public boolean send(RecoveryStore.Delivery delivery) throws IOException, InterruptedException {
        String link = origin + "/recuperar.html?redefinir#" + delivery.token();
        String body = json.writeValueAsString(Map.of(
                "from", from, "to", List.of(delivery.email()), "subject", "ASCEND | Recuperação de senha",
                "html", template.replace("{{link}}", link),
                "text", "Recupere o acesso à sua conta.\n\nRecebemos uma solicitação para redefinir sua senha. "
                        + "Acesse o link para recuperar o acesso à sua conta e continuar sua evolução.\n\n"
                        + link + "\n\nEste link possui validade de 15 minutos. Caso você não tenha solicitado "
                        + "a recuperação, ignore este e-mail.\n\nASCEND — Sua rotina. Sua evolução.\nEVOLVE ◆ CONQUER ◆ ASCEND"));
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + key)
                .header("Idempotency-Key", delivery.lease().toString())
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        int status = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
        return status >= 200 && status < 300;
    }
}

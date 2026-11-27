package br.com.ascend.the_system.service;

import br.com.ascend.the_system.controller.CosmeticStoreController;
import br.com.ascend.the_system.dto.EquipCosmeticRequest;
import br.com.ascend.the_system.dto.ShopPurchaseRequest;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest(properties = {"ascend.recovery.worker-initial-delay-ms=3600000", "ASCEND_HASH_NEW_SESSION_TOKENS=true"})
@AutoConfigureMockMvc
@Import(PasswordRecoveryPostgresTest.MailConfig.class)
@EnabledIfEnvironmentVariable(named = "ASCEND_RECOVERY_TEST_PORT", matches = "[0-9]{1,5}")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PasswordRecoveryPostgresTest {
    private static final BlockingQueue<String> MAIL = new LinkedBlockingQueue<>();
    private static final AtomicInteger MAIL_STATUS = new AtomicInteger(200);
    private static HttpServer smtpApi;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired RecoveryDeliveryWorker worker;
    @Autowired CosmeticStoreController cosmetics;
    private final String email = "jogador@example.test";
    private final String previous = "Senha-Anterior32";
    private final String next = "Minha-nova-senha123";
    private String ip;
    private long id;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry props) throws Exception {
        String base = "jdbc:postgresql://127.0.0.1:" + Integer.parseInt(System.getenv("ASCEND_RECOVERY_TEST_PORT")) + "/";
        String db = "recovery_test_" + UUID.randomUUID().toString().replace("-", "");
        try (var conn = DriverManager.getConnection(base + "postgres", "ascend_test", "local-recovery-only");
             var stmt = conn.createStatement()) {
            stmt.execute("create database " + db);
        }
        props.add("spring.datasource.url", () -> base + db);
        props.add("spring.datasource.username", () -> "ascend_test");
        props.add("spring.datasource.password", () -> "local-recovery-only");
        props.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        props.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        props.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @TestConfiguration
    static class MailConfig {
        @Bean @Primary
        RecoveryMailer localResend(ObjectMapper json) throws IOException {
            smtpApi = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            smtpApi.createContext("/emails", exchange -> {
                if (!"POST".equals(exchange.getRequestMethod())
                        || !"Bearer test-only".equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
                    exchange.sendResponseHeaders(403, -1);
                } else {
                    MAIL.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                    exchange.sendResponseHeaders(MAIL_STATUS.get(), -1);
                }
                exchange.close();
            });
            smtpApi.start();
            return new RecoveryMailer(json, "test-only", "ASCEND <recovery@example.test>", "https://ascend.example",
                    URI.create("http://127.0.0.1:" + smtpApi.getAddress().getPort() + "/emails"));
        }
    }

    @BeforeAll
    void migrateWithExistingAccounts() throws Exception {
        register("outro@example.test");
        register(email); // ID 2 has the Founder badge in the existing policy.
        String before = jdbc.queryForObject("select jsonb_agg(to_jsonb(u) order by id)::text from users u", String.class);
        jdbc.execute(Files.readString(Path.of("database/migrations/2026-11-27-password-recovery.sql")));
        assertEquals(before, jdbc.queryForObject("select jsonb_agg(to_jsonb(u) order by id)::text from users u", String.class));
    }

    @AfterAll
    void shutdown() {
        if (smtpApi != null) smtpApi.stop(0);
    }

    @BeforeEach
    void prepare() {
        jdbc.update("delete from ascend_recovery.deliveries");
        jdbc.update("delete from ascend_recovery.limits");
        MAIL.clear();
        MAIL_STATUS.set(200);
        ip = "127.0.0.2";
        id = jdbc.queryForObject("select id from users where email = ?", Long.class, email);
    }

    @Test
    void replacesPasswordOnceAndPreservesAccountAndOtherSessions() throws Exception {
        String session = login(previous, 200);
        String legacy = UUID.randomUUID().toString();
        jdbc.update("insert into user_sessions (user_id,token,created_at,expires_at) values(?,?,now(),now()+interval '1 day')", id, legacy);
        String otherSession = register("terceiro@example.test");
        jdbc.update("update users set coins=2712, level=5, experience=17 where id=?", id);
        String header = "Bearer " + session;
        cosmetics.purchase(header, new ShopPurchaseRequest("genesis-frame-01"));
        cosmetics.purchase(header, new ShopPurchaseRequest("genesis-frame-02"));
        cosmetics.equip(header, new EquipCosmeticRequest("genesis-frame-02"));
        assertEquals(200, mvc.perform(get("/api/player/status").header("Authorization", header)).andReturn().getResponse().getStatus());
        var inventory = cosmetics.inventory(header);
        var equipped = cosmetics.equipped(header);
        String history = purchaseHistory();
        String userBefore = snapshot();
        String otherHash = jdbc.queryForObject("select password_hash from users where email='outro@example.test'", String.class);
        var known = postJson("forgot-password", Map.of("email", email.toUpperCase()), 202);
        assertEquals(known, postJson("forgot-password", Map.of("email", "ausente@example.test"), 202));
        worker.dispatch();
        String token = mailedToken();
        assertNull(MAIL.poll());
        String hash = jdbc.queryForObject("select token_hash from ascend_recovery.tokens where user_id=? and revoked_at is null and used_at is null", String.class, id);
        assertEquals(RecoveryLimits.digest(token), hash);
        postJson("reset-password/validate", Map.of("token", token), 204);
        reset(token, next, previous, 400);
        reset(token, next, next, 204);
        reset(token, next, next, 400);
        postJson("reset-password/validate", Map.of("token", token), 400);
        login(previous, 401);
        String fresh = login(next, 200);
        for (String old : List.of(session, legacy)) assertEquals(401, me(old));
        assertEquals(200, me(fresh));
        assertEquals(200, me(otherSession));
        String freshHeader = "Bearer " + fresh;
        assertEquals(inventory, cosmetics.inventory(freshHeader));
        assertEquals(equipped, cosmetics.equipped(freshHeader));
        assertEquals(history, purchaseHistory());
        assertTrue(FounderBadgePolicy.isFounder(id));
        assertEquals(userBefore, snapshot());
        assertEquals(otherHash, jdbc.queryForObject("select password_hash from users where email='outro@example.test'", String.class));
    }

    @Test
    void rejectsExpiredSupersededAndMalformedLinks() throws Exception {
        String first = issue();
        String second = issue();
        reset(first, next, next, 400);
        jdbc.update("update ascend_recovery.tokens set created_at=created_at-interval '16 minutes', expires_at=expires_at-interval '16 minutes' where token_hash=?", RecoveryLimits.digest(second));
        reset(second, next, next, 400);
        reset("invalid", next, next, 400);
        reset("Z".repeat(43), next, next, 400);
    }

    @Test
    void twoConcurrentRedemptionsHaveOneWinner() throws Exception {
        String token = issue();
        var start = new CyclicBarrier(2);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Callable<Integer> submit = () -> {
                start.await(5, TimeUnit.SECONDS);
                return send("reset-password", Map.of("token", token, "newPassword", previous, "confirmPassword", previous)).getStatus();
            };
            var first = pool.submit(submit);
            var second = pool.submit(submit);
            assertEquals(List.of(204, 400), java.util.stream.Stream.of(first.get(10, TimeUnit.SECONDS),
                    second.get(10, TimeUnit.SECONDS)).sorted().toList());
        }
    }

    @Test
    void providerFailureRevokesOnlyFailedLinkAndRetries() throws Exception {
        MAIL_STATUS.set(503);
        String failed = issue();
        reset(failed, next, next, 400);
        jdbc.update("update ascend_recovery.deliveries set next_attempt=now() where user_id=?", id);
        MAIL_STATUS.set(200);
        worker.dispatch();
        String retried = mailedToken();
        assertNotEquals(failed, retried);
        postJson("reset-password/validate", Map.of("token", retried), 204);
        assertEquals(0, jdbc.queryForObject("select count(*) from ascend_recovery.deliveries", Integer.class));
    }

    @Test
    void accountAndIpLimitsSurviveIndependentRequests() throws Exception {
        String token = null;
        for (int i = 0; i < 3; i++) {
            ip = "127.0.1." + (i + 1);
            token = issue();
        }
        ip = "127.0.1.4";
        postJson("forgot-password", Map.of("email", email), 202);
        worker.dispatch();
        assertNull(MAIL.poll());
        for (int i = 0; i < 8; i++) reset(token, next, previous, 400);
        reset(token, next, next, 429);
        ip = "127.0.0.3";
        for (int i = 0; i < 20; i++) postJson("forgot-password", Map.of("email", "missing" + i + "@example.test"), 202);
        postJson("forgot-password", Map.of("email", email), 429);
        for (int i = 0; i < 30; i++) reset("unknown", next, next, 400);
        reset("unknown", next, next, 429);
    }

    private String snapshot() {
        return jdbc.queryForObject("select (to_jsonb(u) - 'password_hash' - 'row_version')::text from users u where id=?", String.class, id);
    }

    private String purchaseHistory() {
        return jdbc.queryForObject("select jsonb_agg(to_jsonb(p) order by id)::text "
                + "from cosmetic_purchase_history p where user_id=?", String.class, id);
    }

    private String issue() throws Exception {
        postJson("forgot-password", Map.of("email", email), 202);
        worker.dispatch();
        return mailedToken();
    }

    private String mailedToken() throws Exception {
        String raw = MAIL.poll(2, TimeUnit.SECONDS);
        assertNotNull(raw);
        JsonNode mail = json.readTree(raw);
        assertEquals(email, mail.get("to").get(0).asText());
        assertEquals("ASCEND | Recuperação de senha", mail.get("subject").asText());
        String link = mail.get("text").asText().lines().filter(line -> line.startsWith("https://")).findFirst().orElseThrow();
        assertTrue(link.startsWith("https://ascend.example/recuperar.html?redefinir#"));
        assertTrue(mail.get("html").asText().contains("href=\"" + link + "\""));
        return URI.create(link).getFragment();
    }

    private int me(String token) throws Exception {
        return mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token)).andReturn().getResponse().getStatus();
    }

    private String register(String address) throws Exception {
        var result = postJson("register", Map.of("email", address, "name", "Jogador", "password", previous), 201);
        return json.readTree(result).get("token").asText();
    }

    private String login(String password, int expected) throws Exception {
        var result = postJson("login", Map.of("email", email, "password", password), expected);
        return expected == 200 ? json.readTree(result).get("token").asText() : "";
    }

    private void reset(String token, String password, String confirm, int status) throws Exception {
        postJson("reset-password", Map.of("token", token, "newPassword", password, "confirmPassword", confirm), status);
    }

    private String postJson(String path, Map<String,String> data, int expected) throws Exception {
        var response = send(path, data);
        assertEquals(expected, response.getStatus(), path);
        return response.getContentAsString(StandardCharsets.UTF_8);
    }

    private org.springframework.mock.web.MockHttpServletResponse send(String path, Map<String,String> data) throws Exception {
        return mvc.perform(post("/api/auth/" + path).with(request -> {
            request.setRemoteAddr(ip == null ? "127.0.0.1" : ip);
            return request;
        }).header("X-Forwarded-Host", "untrusted.invalid").contentType("application/json")
          .content(json.writeValueAsString(data))).andReturn().getResponse();
    }
}

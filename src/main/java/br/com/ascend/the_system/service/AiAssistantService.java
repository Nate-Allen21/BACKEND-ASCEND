package br.com.ascend.the_system.service;

import br.com.ascend.the_system.domain.User;
import br.com.ascend.the_system.domain.UserQuest;
import br.com.ascend.the_system.domain.VaultTransaction;
import br.com.ascend.the_system.dto.AiChatMessage;
import br.com.ascend.the_system.dto.AiChatRequest;
import br.com.ascend.the_system.dto.AiChatResponse;
import br.com.ascend.the_system.dto.PlayerStatusResponse;
import br.com.ascend.the_system.dto.ProductivityPlanResponse;
import br.com.ascend.the_system.dto.RoutineProfileRequest;
import br.com.ascend.the_system.repository.UserQuestRepository;
import br.com.ascend.the_system.repository.VaultTransactionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class AiAssistantService {

    private static final String ASSISTANT_NAME = "ASCEND AI";

    private final HttpClient httpClient;
    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final String provider;
    private final String responseMode;
    private final String apiVersion;
    private final String deployment;
    private final PlayerStatusService playerStatusService;
    private final UserQuestRepository userQuestRepository;
    private final VaultTransactionRepository vaultTransactionRepository;

    public AiAssistantService(
            PlayerStatusService playerStatusService,
            UserQuestRepository userQuestRepository,
            VaultTransactionRepository vaultTransactionRepository,
            @Value("${ascend.ai.api-key:}") String apiKey,
            @Value("${ascend.ai.model:gpt-4.1-mini}") String model,
            @Value("${ascend.ai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${ascend.ai.provider:openai}") String provider,
            @Value("${ascend.ai.response-mode:chat-completions}") String responseMode,
            @Value("${ascend.ai.api-version:}") String apiVersion,
            @Value("${ascend.ai.deployment:}") String deployment
    ) {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
        this.apiKey = resolveApiKey(apiKey, provider);
        this.model = model == null ? "" : model.trim();
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.provider = normalizeProvider(provider);
        this.responseMode = normalizeResponseMode(responseMode);
        this.apiVersion = apiVersion == null ? "" : apiVersion.trim();
        this.deployment = deployment == null ? "" : deployment.trim();
        this.playerStatusService = playerStatusService;
        this.userQuestRepository = userQuestRepository;
        this.vaultTransactionRepository = vaultTransactionRepository;
    }

    public AiChatResponse chat(User user, AiChatRequest request) {
        String message = request == null || request.message() == null ? "" : request.message().trim();
        RoutineProfileRequest profile = request == null ? null : request.profile();

        if (!hasApiKey()) {
            return fallbackChat(user, message, profile);
        }

        try {
            return callOpenAi(user, message, request == null ? List.of() : safeConversation(request.conversation()), profile);
        } catch (UnauthorizedAiException error) {
            return new AiChatResponse(
                    ASSISTANT_NAME,
                    "A IA externa foi configurada, mas a chave atual foi recusada pelo provedor. " +
                    "Verifique se `ASCEND_AI_KEY` é uma chave válida e se `ASCEND_AI_BASE_URL` aponta para o endpoint correto.\n\n" +
                            "Enquanto isso, eu continuo disponível no modo local.",
                    List.of("Revisar chave", "Ver calendario", "Gerar plano")
            );
        } catch (Exception error) {
            return fallbackChat(user, message, profile);
        }
    }

    private AiChatResponse callOpenAi(User user, String message, List<AiChatMessage> conversation, RoutineProfileRequest profile) throws Exception {
        String payload = buildPayload(user, message, conversation, profile);
        String endpoint = resolveRequestEndpoint(baseUrl, provider, responseMode, apiVersion, deployment);
        boolean useResponsesApi = shouldUseResponsesApi(responseMode);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(45))
                .header("Content-Type", "application/json");

        if (shouldUseAzureOpenAi(provider)) {
            requestBuilder.header("api-key", apiKey);
        } else {
            requestBuilder.header("Authorization", "Bearer " + apiKey);
        }

        HttpRequest request = requestBuilder.POST(HttpRequest.BodyPublishers.ofString(payload)).build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new UnauthorizedAiException("AI provider rejected credentials: " + response.statusCode());
        }
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("OpenAI request failed: " + response.statusCode());
        }

        String reply = useResponsesApi ? extractResponsesApiContent(response.body()) : extractChatCompletionContent(response.body());

        return new AiChatResponse(
                ASSISTANT_NAME,
                reply,
                List.of(
                        "Abrir calendário da semana",
                        "Gerar próximas missões",
                        "Ajustar foco do dia"
                )
        );
    }

    private String buildPayload(User user, String message, List<AiChatMessage> conversation, RoutineProfileRequest profile) {
        if (shouldUseResponsesApi(responseMode)) {
            return buildResponsesPayload(user, message, conversation, profile);
        }

        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append("\"model\":\"").append(escapeJson(model)).append("\",");
        builder.append("\"messages\":[");
        appendMessage(builder, "system", """
                Você é o ASCEND AI, um assistente de produtividade e rotina.
                Responda em português do Brasil.
                Seja prático, claro e orientado para a ação.
                Ajude com planejamento de rotina, estudo, treino, trabalho e calendário.
                Você tem acesso ao snapshot do sistema, aos eventos do dia, ao progresso recente e ao histórico resumido.
                Evite respostas longas demais.
                """);
        builder.append(',');
        appendMessage(builder, "system", "Contexto do usuário: " + user.getName() + ", rank " + user.getRank() + ", nível " + user.getLevel() + ", streak " + user.getStreakDays() + ".");
        builder.append(',');
        appendMessage(builder, "system", buildSystemSnapshot(user, profile));
        if (profile != null) {
            builder.append(',');
            appendMessage(builder, "system", "Perfil de rotina: " + playerStatusService.describeProfile(profile));
        }
        for (AiChatMessage chatMessage : conversation) {
            if (chatMessage == null || chatMessage.content() == null || chatMessage.content().isBlank()) {
                continue;
            }
            builder.append(',');
            appendMessage(builder, normalizeRole(chatMessage.role()), chatMessage.content());
        }
        builder.append(',');
        appendMessage(builder, "user", message);
        builder.append("],");
        builder.append("\"temperature\":0.7");
        builder.append("}");
        return builder.toString();
    }

    private String buildResponsesPayload(User user, String message, List<AiChatMessage> conversation, RoutineProfileRequest profile) {
        StringBuilder input = new StringBuilder();
        input.append("Você é o ASCEND AI, um assistente de produtividade e rotina.\n");
        input.append("Responda em português do Brasil e seja prático, claro e orientado para a ação.\n");
        input.append("Contexto do usuário: ").append(user.getName()).append(", rank ").append(user.getRank()).append(", nível ").append(user.getLevel()).append(", streak ").append(user.getStreakDays()).append(".\n");
        input.append(buildSystemSnapshot(user, profile));
        if (profile != null) {
            input.append("Perfil de rotina: ").append(playerStatusService.describeProfile(profile)).append("\n");
        }
        for (AiChatMessage chatMessage : conversation) {
            if (chatMessage == null || chatMessage.content() == null || chatMessage.content().isBlank()) {
                continue;
            }
            input.append(normalizeRole(chatMessage.role())).append(": ").append(chatMessage.content()).append("\n");
        }
        input.append("user: ").append(message);

        return "{\"model\":\"" + escapeJson(model) + "\",\"input\":\"" + escapeJson(input.toString()) + "\"}";
    }

    private void appendMessage(StringBuilder builder, String role, String content) {
        builder.append("{\"role\":\"")
                .append(escapeJson(role))
                .append("\",\"content\":\"")
                .append(escapeJson(content))
                .append("\"}");
    }

    private String extractChatCompletionContent(String body) {
        int messageIndex = body.indexOf("\"message\"");
        if (messageIndex < 0) {
            return "A IA nao retornou conteudo.";
        }

        int contentIndex = body.indexOf("\"content\"", messageIndex);
        if (contentIndex < 0) {
            return "A IA nao retornou conteudo.";
        }

        int firstQuote = body.indexOf('"', contentIndex + 9);
        if (firstQuote < 0) {
            return "A IA nao retornou conteudo.";
        }

        return unescapeJsonString(body, firstQuote + 1);
    }

    private String extractResponsesApiContent(String body) {
        int outputTextIndex = body.indexOf("\"output_text\"");
        if (outputTextIndex >= 0) {
            int firstQuote = body.indexOf('"', outputTextIndex + 14);
            if (firstQuote >= 0) {
                return unescapeJsonString(body, firstQuote + 1);
            }
        }

        int outputIndex = body.indexOf("\"output\"");
        if (outputIndex >= 0) {
            int textIndex = body.indexOf("\"text\"", outputIndex);
            if (textIndex >= 0) {
                int firstQuote = body.indexOf('"', textIndex + 7);
                if (firstQuote >= 0) {
                    return unescapeJsonString(body, firstQuote + 1);
                }
            }
        }

        return extractChatCompletionContent(body);
    }

    private String unescapeJsonString(String value, int startIndex) {
        StringBuilder result = new StringBuilder();
        boolean escaping = false;

        for (int index = startIndex; index < value.length(); index++) {
            char current = value.charAt(index);
            if (escaping) {
                switch (current) {
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case 'b' -> result.append('\b');
                    case 'f' -> result.append('\f');
                    case '\\' -> result.append('\\');
                    case '"' -> result.append('"');
                    case '/' -> result.append('/');
                    case 'u' -> {
                        if (index + 4 < value.length()) {
                            String hex = value.substring(index + 1, index + 5);
                            result.append((char) Integer.parseInt(hex, 16));
                            index += 4;
                        }
                    }
                    default -> result.append(current);
                }
                escaping = false;
                continue;
            }

            if (current == '\\') {
                escaping = true;
                continue;
            }
            if (current == '"') {
                break;
            }
            result.append(current);
        }

        return result.toString().trim();
    }

    private String escapeJson(String value) {
        return String.valueOf(value)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private List<AiChatMessage> safeConversation(List<AiChatMessage> conversation) {
        return conversation == null ? List.of() : conversation;
    }

    private boolean hasApiKey() {
        return !apiKey.isBlank();
    }

    private AiChatResponse fallbackChat(User user, String message, RoutineProfileRequest profile) {
        String focus = playerStatusService.describeProfile(profile);
        if (focus == null || focus.isBlank()) {
            focus = "sua rotina atual";
        }
        String reply = "Modo local ativo. Eu ainda posso te ajudar com base no seu perfil, mas para respostas de IA real configure `ASCEND_AI_KEY`.\n\n"
                + "Com base no seu contexto, eu sugiro:\n"
                + "1. Escolher uma prioridade para hoje.\n"
                + "2. Fechar um bloco de foco de 25 a 45 minutos.\n"
                + "3. Registrar o resultado no calendario.\n\n"
                + "Contexto lido: " + focus + ".";

        if (message != null && !message.isBlank()) {
            reply += "\n\nVoce perguntou: " + message;
        }

        return new AiChatResponse(
                ASSISTANT_NAME,
                reply,
                List.of("Ver calendario", "Gerar plano", "Ajustar rotina")
        );
    }

    private String buildSystemSnapshot(User user, RoutineProfileRequest profile) {
        PlayerStatusResponse status = playerStatusService.getCurrentStatus(user);
        ProductivityPlanResponse plan = playerStatusService.generateAdaptivePlan(user, profile);
        List<UserQuest> quests = safeList(userQuestRepository.findTop10ByUser_IdOrderByScheduledDateDesc(user.getId()));
        List<VaultTransaction> transactions = safeList(vaultTransactionRepository.findTop10ByUser_IdOrderByCreatedAtDesc(user.getId()));

        StringBuilder builder = new StringBuilder();
        builder.append("Snapshot do sistema:\n");
        builder.append("status=").append(status).append("\n");
        builder.append("plano=").append(plan).append("\n");
        builder.append("quests=").append(formatQuests(quests)).append("\n");
        builder.append("transacoes=").append(formatTransactions(transactions)).append("\n");
        return builder.toString();
    }

    private String formatQuests(List<UserQuest> quests) {
        if (quests == null || quests.isEmpty()) {
            return "nenhuma";
        }

        List<String> lines = new ArrayList<>();
        for (UserQuest quest : quests) {
            if (quest == null || quest.getMission() == null) {
                continue;
            }
            lines.add(quest.getScheduledDate()
                    + " | " + quest.getMission().getTitle()
                    + " | " + quest.getStatus()
                    + " | exp=" + quest.getGrantedExperience()
                    + " | coins=" + quest.getGrantedCoins());
        }
        return lines.isEmpty() ? "nenhuma" : String.join(" || ", lines);
    }

    private String formatTransactions(List<VaultTransaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return "nenhuma";
        }

        List<String> lines = new ArrayList<>();
        for (VaultTransaction transaction : transactions) {
            if (transaction == null) {
                continue;
            }
            lines.add(transaction.getType()
                    + " | " + transaction.getDescription()
                    + " | amount=" + transaction.getAmount());
        }
        return lines.isEmpty() ? "nenhuma" : String.join(" || ", lines);
    }

    private String normalizeRole(String role) {
        return "assistant".equalsIgnoreCase(role) ? "assistant" : "user";
    }

    private <T> List<T> safeList(List<T> list) {
        return list == null ? List.of() : list;
    }

    private boolean shouldUseResponsesApi(String responseMode) {
        String normalized = normalizeResponseMode(responseMode);
        return "responses".equals(normalized) || "responses-api".equals(normalized);
    }

    private boolean shouldUseAzureOpenAi(String provider) {
        String normalized = normalizeProvider(provider);
        return "azure-openai".equals(normalized) || "azure".equals(normalized);
    }

    static String resolveRequestEndpoint(String configuredBaseUrl, String provider, String responseMode, String apiVersion, String deployment) {
        String base = configuredBaseUrl == null ? "" : configuredBaseUrl.trim();
        if (base.isBlank()) {
            base = "https://api.openai.com/v1";
        }
        base = base.replaceAll("/+$", "");

        String normalizedProvider = normalizeProvider(provider);
        boolean useResponsesApi = isResponsesMode(responseMode);

        if (isAzureOpenAi(normalizedProvider)) {
            String endpoint = base;
            if (!deployment.isBlank() && !endpoint.contains("/deployments/")) {
                endpoint += "/deployments/" + deployment.trim();
            }
            endpoint += useResponsesApi ? "/responses" : "/chat/completions";
            if (!apiVersion.isBlank()) {
                endpoint += (endpoint.contains("?") ? "&" : "?") + "api-version=" + URLEncoder.encode(apiVersion.trim(), StandardCharsets.UTF_8);
            }
            return endpoint;
        }

        return base + (useResponsesApi ? "/responses" : "/chat/completions");
    }

    static String resolveApiKey(String configuredApiKey, String provider) {
        if (configuredApiKey != null && !configuredApiKey.isBlank()) {
            return configuredApiKey.trim();
        }

        String genericAscendAiApiKey = System.getenv("ASCEND_AI_API_KEY");
        if (genericAscendAiApiKey != null && !genericAscendAiApiKey.isBlank()) {
            return genericAscendAiApiKey.trim();
        }

        String genericAscendAiKey = System.getenv("ASCEND_AI_KEY");
        if (genericAscendAiKey != null && !genericAscendAiKey.isBlank()) {
            return genericAscendAiKey.trim();
        }

        String normalizedProvider = normalizeProvider(provider);
        String envVarName = resolveProviderApiKeyEnvironmentVariable(normalizedProvider);
        String fromEnv = System.getenv(envVarName);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }

        if (isLlama(normalizedProvider)) {
            return "";
        }

        if (isAzureOpenAi(normalizedProvider)) {
            String openAiFallback = System.getenv("OPENAI_API_KEY");
            if (openAiFallback != null && !openAiFallback.isBlank()) {
                return openAiFallback.trim();
            }
        }

        return "";
    }

    private static String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl == null ? "" : baseUrl.trim();
        if (normalized.isBlank()) {
            return "https://api.openai.com/v1";
        }
        return normalized.replaceAll("/+$", "");
    }

    private static String normalizeProvider(String provider) {
        String normalized = provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? "openai" : normalized;
    }

    private static String normalizeResponseMode(String responseMode) {
        String normalized = responseMode == null ? "" : responseMode.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? "chat-completions" : normalized;
    }

    private static boolean isResponsesMode(String responseMode) {
        String normalized = normalizeResponseMode(responseMode);
        return "responses".equals(normalized) || "responses-api".equals(normalized);
    }

    private static boolean isAzureOpenAi(String provider) {
        String normalized = normalizeProvider(provider);
        return "azure-openai".equals(normalized) || "azure".equals(normalized);
    }

    private static boolean isLlama(String provider) {
        String normalized = normalizeProvider(provider);
        return "llama".equals(normalized) || "meta-llama".equals(normalized) || "meta".equals(normalized);
    }

    private static String resolveProviderApiKeyEnvironmentVariable(String provider) {
        if (isAzureOpenAi(provider)) {
            return "AZURE_OPENAI_API_KEY";
        }
        if (isLlama(provider)) {
            return "LLAMA_API_KEY";
        }
        return "OPENAI_API_KEY";
    }

    private static final class UnauthorizedAiException extends RuntimeException {
        private UnauthorizedAiException(String message) {
            super(message);
        }
    }
}

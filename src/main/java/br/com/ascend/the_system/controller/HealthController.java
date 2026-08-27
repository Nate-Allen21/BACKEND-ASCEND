package br.com.ascend.the_system.controller;

import br.com.ascend.the_system.dto.HealthResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final String applicationName;
    private final String aiProvider;
    private final String aiBaseUrl;
    private final String aiModel;
    private final String aiResponseMode;
    private final String aiApiKey;

    public HealthController(
            @Value("${spring.application.name}") String applicationName,
            @Value("${ascend.ai.provider:}") String aiProvider,
            @Value("${ascend.ai.base-url:}") String aiBaseUrl,
            @Value("${ascend.ai.model:}") String aiModel,
            @Value("${ascend.ai.response-mode:}") String aiResponseMode,
            @Value("${ascend.ai.api-key:}") String aiApiKey
    ) {
        this.applicationName = applicationName;
        this.aiProvider = aiProvider;
        this.aiBaseUrl = aiBaseUrl;
        this.aiModel = aiModel;
        this.aiResponseMode = aiResponseMode;
        this.aiApiKey = aiApiKey;
    }

    @GetMapping
    public HealthResponse health() {
        return new HealthResponse("UP", applicationName, OffsetDateTime.now());
    }

    @GetMapping("/ai")
    public Map<String, Object> aiHealth() {
        return Map.of(
                "status", "UP",
                "provider", aiProvider,
                "baseUrl", aiBaseUrl,
                "model", aiModel,
                "responseMode", aiResponseMode,
                "apiKeyConfigured", aiApiKey != null && !aiApiKey.isBlank()
        );
    }
}

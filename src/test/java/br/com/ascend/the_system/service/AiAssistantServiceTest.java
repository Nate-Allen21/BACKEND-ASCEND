package br.com.ascend.the_system.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiAssistantServiceTest {

    @Test
    void shouldBuildAzureChatCompletionsEndpoint() {
        String endpoint = AiAssistantService.resolveRequestEndpoint(
                "https://example-resource.openai.azure.com/openai/deployments/gpt-4o",
                "azure-openai",
                "chat-completions",
                "2024-10-21",
                "gpt-4o"
        );

        assertEquals(
                "https://example-resource.openai.azure.com/openai/deployments/gpt-4o/chat/completions?api-version=2024-10-21",
                endpoint
        );
    }

    @Test
    void shouldBuildOpenAiCompatibleResponsesEndpoint() {
        String endpoint = AiAssistantService.resolveRequestEndpoint(
                "https://llm.example.com/v1",
                "openai-compatible",
                "responses",
                "",
                ""
        );

        assertEquals("https://llm.example.com/v1/responses", endpoint);
    }

    @Test
    void shouldPreferConfiguredApiKeyOverEnvironmentFallback() {
        String key = AiAssistantService.resolveApiKey("configured-key", "openai");

        assertEquals("configured-key", key);
    }

    @Test
    void shouldBuildGroqChatCompletionsEndpointFromBlankBaseUrl() {
        String endpoint = AiAssistantService.resolveRequestEndpoint(
                "",
                "groq",
                "chat-completions",
                "",
                ""
        );

        assertEquals("https://api.groq.com/openai/v1/chat/completions", endpoint);
    }

    @Test
    void shouldBuildGroqEndpointFromConfiguredBaseUrl() {
        String endpoint = AiAssistantService.resolveRequestEndpoint(
                "https://api.groq.com/openai/v1/",
                "groq",
                "chat-completions",
                "",
                ""
        );

        assertEquals("https://api.groq.com/openai/v1/chat/completions", endpoint);
    }

    @Test
    void shouldResolveProviderDefaultBaseUrl() {
        assertEquals("https://api.groq.com/openai/v1", AiAssistantService.resolveDefaultBaseUrl("groq"));
        assertEquals("https://api.groq.com/openai/v1", AiAssistantService.resolveDefaultBaseUrl("GROQ"));
        assertEquals("https://api.openai.com/v1", AiAssistantService.resolveDefaultBaseUrl("openai"));
    }
}

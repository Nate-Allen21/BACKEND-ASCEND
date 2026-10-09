package br.com.ascend.the_system.controller;

import br.com.ascend.the_system.dto.AiChatRequest;
import br.com.ascend.the_system.dto.AiChatResponse;
import br.com.ascend.the_system.service.AiAssistantService;
import br.com.ascend.the_system.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiAssistantService aiAssistantService;
    private final AuthService authService;

    public AiController(AiAssistantService aiAssistantService, AuthService authService) {
        this.aiAssistantService = aiAssistantService;
        this.authService = authService;
    }

    @PostMapping("/chat")
    public AiChatResponse chat(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody(required = false) AiChatRequest request
    ) {
        return aiAssistantService.chat(authService.resolveUser(authorizationHeader), request);
    }
}

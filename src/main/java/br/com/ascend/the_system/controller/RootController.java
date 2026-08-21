package br.com.ascend.the_system.controller;

import br.com.ascend.the_system.dto.HealthResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
public class RootController {

    private final String applicationName;

    public RootController(@Value("${spring.application.name}") String applicationName) {
        this.applicationName = applicationName;
    }

    @GetMapping("/")
    public HealthResponse root() {
        return new HealthResponse("UP", applicationName, OffsetDateTime.now());
    }
}

package br.com.ascend.the_system.controller;

import br.com.ascend.the_system.dto.AuthRequest;
import br.com.ascend.the_system.dto.AuthResponse;
import br.com.ascend.the_system.dto.AvatarSelectionRequest;
import br.com.ascend.the_system.dto.RegisterRequest;
import br.com.ascend.the_system.dto.UserResponse;
import br.com.ascend.the_system.dto.GoogleAuthRequest;
import br.com.ascend.the_system.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest request) {
        return authService.login(request);
    }

    @PostMapping("/google")
    public AuthResponse google(@RequestBody GoogleAuthRequest request) {
        return authService.googleLogin(request);
    }

    @GetMapping("/me")
    public UserResponse me(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return authService.me(authorizationHeader);
    }

    @PutMapping("/avatar")
    public UserResponse selectAvatar(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody AvatarSelectionRequest request
    ) {
        return authService.selectAvatar(authorizationHeader, request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        authService.logout(authorizationHeader);
    }
}

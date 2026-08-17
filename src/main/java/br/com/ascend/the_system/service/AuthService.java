package br.com.ascend.the_system.service;

import br.com.ascend.the_system.domain.User;
import br.com.ascend.the_system.domain.UserSession;
import br.com.ascend.the_system.dto.AuthRequest;
import br.com.ascend.the_system.dto.AuthResponse;
import br.com.ascend.the_system.dto.RegisterRequest;
import br.com.ascend.the_system.dto.UserResponse;
import br.com.ascend.the_system.exception.BadRequestException;
import br.com.ascend.the_system.exception.ConflictException;
import br.com.ascend.the_system.exception.UnauthorizedException;
import br.com.ascend.the_system.repository.UserRepository;
import br.com.ascend.the_system.repository.UserSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private static final int SESSION_DAYS = 7;

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordService passwordService;

    public AuthService(
            UserRepository userRepository,
            UserSessionRepository userSessionRepository,
            PasswordService passwordService
    ) {
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.passwordService = passwordService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request == null) {
            throw new BadRequestException("Informe os dados de cadastro.");
        }

        validateName(request.name());
        validateEmail(request.email());
        validatePassword(request.password());

        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Este e-mail já está cadastrado.");
        }

        User user = new User(request.name().trim(), email, passwordService.hash(request.password()));
        userRepository.save(user);

        return createSession(user);
    }

    @Transactional
    public AuthResponse login(AuthRequest request) {
        if (request == null) {
            throw new BadRequestException("Informe e-mail e senha.");
        }

        validateEmail(request.email());
        validatePasswordPresence(request.password());

        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .filter(foundUser -> passwordService.matches(request.password(), foundUser.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException("E-mail ou senha inválidos."));

        return createSession(user);
    }

    @Transactional(readOnly = true)
    public UserResponse me(String authorizationHeader) {
        return toUserResponse(resolveUser(authorizationHeader));
    }

    @Transactional
    public void logout(String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        userSessionRepository.deleteByToken(token);
    }

    @Transactional(readOnly = true)
    public User resolveUser(String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        UserSession session = userSessionRepository.findByToken(token)
                .orElseThrow(() -> new UnauthorizedException("Sessão inválida ou expirada."));

        if (session.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new UnauthorizedException("Sessão inválida ou expirada.");
        }

        return session.getUser();
    }

    private AuthResponse createSession(User user) {
        String token = UUID.randomUUID().toString();
        UserSession session = new UserSession(token, user, OffsetDateTime.now().plusDays(SESSION_DAYS));
        userSessionRepository.save(session);

        return new AuthResponse(token, toUserResponse(user));
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getLevel(),
                user.getRank(),
                user.getExperience(),
                user.getNextLevelExperience(),
                user.getStreakDays(),
                user.getCoins()
        );
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Sessão inválida ou expirada.");
        }

        return authorizationHeader.substring("Bearer ".length()).trim();
    }

    private void validateName(String name) {
        if (name == null || name.trim().length() < 2) {
            throw new BadRequestException("Informe um nome com pelo menos 2 caracteres.");
        }
    }

    private void validateEmail(String email) {
        if (email == null || !email.contains("@") || !email.contains(".")) {
            throw new BadRequestException("Informe um e-mail válido.");
        }
    }

    private void validatePassword(String password) {
        validatePasswordPresence(password);
        if (password.length() < 8) {
            throw new BadRequestException("A senha precisa ter pelo menos 8 caracteres.");
        }
    }

    private void validatePasswordPresence(String password) {
        if (password == null || password.isBlank()) {
            throw new BadRequestException("Informe sua senha.");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}

package br.com.ascend.the_system.service;

import br.com.ascend.the_system.domain.User;
import br.com.ascend.the_system.domain.UserSession;
import br.com.ascend.the_system.dto.AvatarSelectionRequest;
import br.com.ascend.the_system.dto.AuthRequest;
import br.com.ascend.the_system.dto.UserResponse;
import br.com.ascend.the_system.exception.BadRequestException;
import br.com.ascend.the_system.repository.UserRepository;
import br.com.ascend.the_system.repository.UserSessionRepository;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

class AuthServiceTest {

    @Test
    void upgradesOnlyAfterSuccessfulAuthentication() {
        UserRepository users = mock(UserRepository.class);
        UserSessionRepository sessions = mock(UserSessionRepository.class);
        PasswordService passwords = mock(PasswordService.class);
        AuthService auth = new AuthService(users, sessions, passwords);
        User user = new User("Hunter", "hunter@example.com", "legacy");
        when(users.findByEmailForUpdate("hunter@example.com")).thenReturn(Optional.of(user));
        when(passwords.matches("correct", "legacy")).thenReturn(true);
        when(passwords.needsUpgrade("legacy")).thenReturn(true);
        when(passwords.hash("correct")).thenReturn("upgraded");
        auth.login(new AuthRequest("hunter@example.com", "correct"));
        assertEquals("upgraded", user.getPasswordHash());
        verify(users).save(user);
        assertThrows(br.com.ascend.the_system.exception.UnauthorizedException.class,
                () -> auth.login(new AuthRequest("hunter@example.com", "wrong")));
        verify(passwords, never()).hash("wrong");
    }

    @Test
    void shouldSaveAvatarForTheAuthenticatedSessionUser() {
        UserRepository userRepository = mock(UserRepository.class);
        UserSessionRepository sessionRepository = mock(UserSessionRepository.class);
        AuthService authService = new AuthService(userRepository, sessionRepository, mock(PasswordService.class));
        User user = new User("Hunter", "hunter@ascend.local", "hash");
        when(sessionRepository.findByToken("session-token"))
                .thenReturn(Optional.of(new UserSession("session-token", user, OffsetDateTime.now().plusDays(1))));
        when(userRepository.save(user)).thenReturn(user);

        UserResponse response = authService.selectAvatar("Bearer session-token", new AvatarSelectionRequest("FEMALE"));

        assertEquals("female", response.avatar());
        assertTrue(response.onboardingCompleted());
        assertEquals("female", user.getAvatar());
        verify(userRepository).save(user);
    }

    @Test
    void shouldRejectUnknownAvatarIdentifiers() {
        UserRepository userRepository = mock(UserRepository.class);
        UserSessionRepository sessionRepository = mock(UserSessionRepository.class);
        AuthService authService = new AuthService(userRepository, sessionRepository, mock(PasswordService.class));

        assertThrows(BadRequestException.class,
                () -> authService.selectAvatar("Bearer session-token", new AvatarSelectionRequest("mage")));
    }
}

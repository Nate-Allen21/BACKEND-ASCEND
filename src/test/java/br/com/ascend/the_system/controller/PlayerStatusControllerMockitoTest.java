package br.com.ascend.the_system.controller;

import br.com.ascend.the_system.domain.User;
import br.com.ascend.the_system.dto.PlayerStatusResponse;
import br.com.ascend.the_system.service.AuthService;
import br.com.ascend.the_system.service.PlayerStatusService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlayerStatusControllerMockitoTest {

    @Test
    void shouldResolveAuthenticatedUserAndDelegateStatusLookup() {
        AuthService authService = mock(AuthService.class);
        PlayerStatusService playerStatusService = mock(PlayerStatusService.class);
        PlayerStatusController controller = new PlayerStatusController(playerStatusService, authService);
        User user = mock(User.class);
        PlayerStatusResponse expected = new PlayerStatusResponse(
                "Hunter", "@hunter", 1, "E", 0, 100, 0, 0,
                List.of(), List.of(), List.of()
        );
        String authorization = "Bearer test-token";

        when(authService.resolveUser(authorization)).thenReturn(user);
        when(playerStatusService.getCurrentStatus(user)).thenReturn(expected);

        assertSame(expected, controller.getStatus(authorization));
        verify(authService).resolveUser(authorization);
        verify(playerStatusService).getCurrentStatus(user);
    }
}

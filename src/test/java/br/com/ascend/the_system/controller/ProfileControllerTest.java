package br.com.ascend.the_system.controller;

import br.com.ascend.the_system.domain.User;
import br.com.ascend.the_system.dto.ProfileUpdateRequest;
import br.com.ascend.the_system.exception.BadRequestException;
import br.com.ascend.the_system.repository.PostCommentRepository;
import br.com.ascend.the_system.repository.PostLikeRepository;
import br.com.ascend.the_system.repository.ProfilePostRepository;
import br.com.ascend.the_system.repository.SocialConnectionRepository;
import br.com.ascend.the_system.repository.UserRepository;
import br.com.ascend.the_system.repository.EquippedCosmeticRepository;
import br.com.ascend.the_system.service.AuthService;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfileControllerTest {

    @Test
    void shouldSaveProfileImageAndBioTogether() {
        ProfileController controller;
        AuthService auth = mock(AuthService.class);
        UserRepository users = mock(UserRepository.class);
        User user = new User("Kael Hunter", "kael@ascend.local", "hash");
        when(auth.resolveUser(anyString())).thenReturn(user);
        controller = new ProfileController(
                auth,
                users,
                mock(ProfilePostRepository.class),
                mock(SocialConnectionRepository.class),
                mock(PostLikeRepository.class),
                mock(PostCommentRepository.class),
                mock(EquippedCosmeticRepository.class)
        );
        String image = "data:image/jpeg;base64," +
                Base64.getEncoder().encodeToString(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});

        Map<String, Object> response = controller.update("Bearer test", new ProfileUpdateRequest(
                "Minha jornada",
                java.util.List.of("Foco"),
                "PUBLIC",
                image
        ));

        assertEquals(image, user.getProfileImage());
        assertEquals(image, response.get("profileImage"));
        assertEquals("Minha jornada", user.getBio());
        verify(users).save(user);
    }

    @Test
    void shouldRejectNonJpegProfileImagesWithoutUpdatingTheUser() {
        AuthService auth = mock(AuthService.class);
        UserRepository users = mock(UserRepository.class);
        User user = new User("Kael Hunter", "kael@ascend.local", "hash");
        when(auth.resolveUser(anyString())).thenReturn(user);
        ProfileController controller = new ProfileController(
                auth,
                users,
                mock(ProfilePostRepository.class),
                mock(SocialConnectionRepository.class),
                mock(PostLikeRepository.class),
                mock(PostCommentRepository.class),
                mock(EquippedCosmeticRepository.class)
        );

        assertThrows(BadRequestException.class, () -> controller.update(
                "Bearer test",
                new ProfileUpdateRequest("Bio", java.util.List.of(), "PUBLIC", "data:image/png;base64,AAAA")
        ));

        verify(users, never()).save(user);
    }
}

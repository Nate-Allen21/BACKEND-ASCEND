package br.com.ascend.the_system.service;

import br.com.ascend.the_system.domain.User;
import br.com.ascend.the_system.dto.AuthRequest;
import br.com.ascend.the_system.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = "ASCEND_UPGRADE_PASSWORD_HASHES=true")
class PasswordUpgradeIntegrationTest {
    @Autowired UserRepository users;
    @Autowired AuthService auth;

    @Test
    void successfulLoginRehashesExistingPasswordWithoutChangingIt() {
        String email = UUID.randomUUID() + "@example.com";
        String legacy = new PasswordService().hash("Password123!");
        users.saveAndFlush(new User("Test User", email, legacy));
        auth.login(new AuthRequest(email, "Password123!"));
        String upgraded = users.findByEmail(email).orElseThrow().getPasswordHash();
        assertTrue(upgraded.startsWith("600000:"));
        assertTrue(new PasswordService().matches("Password123!", upgraded));
        assertNotEquals(legacy, upgraded);
    }
}

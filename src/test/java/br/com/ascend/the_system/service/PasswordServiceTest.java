package br.com.ascend.the_system.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class PasswordServiceTest {
    @Test
    void legacyAndUpgradedHashesRemainVerifiableAcrossRollout() {
        PasswordService oldWriter = new PasswordService();
        String legacy = oldWriter.hash("test-password");
        assertTrue(legacy.startsWith("120000:"));
        PasswordService newWriter = new PasswordService();
        ReflectionTestUtils.setField(newWriter, "upgradePasswordHashes", true);
        assertTrue(newWriter.matches("test-password", legacy));
        assertTrue(newWriter.needsUpgrade(legacy));
        assertFalse(newWriter.matches("wrong-password", legacy));
        String upgraded = newWriter.hash("test-password");
        assertTrue(upgraded.startsWith("600000:"));
        assertTrue(oldWriter.matches("test-password", upgraded));
        assertFalse(newWriter.needsUpgrade(upgraded));
    }

    @Test
    void malformedAndExcessiveHashesFailClosed() {
        PasswordService service = new PasswordService();
        assertFalse(service.matches("password", null));
        assertFalse(service.matches("password", "not-a-hash"));
        assertFalse(service.matches("password", "999999999:bad:bad"));
        assertFalse(service.matches("password", "120000:bad:bad"));
    }
}

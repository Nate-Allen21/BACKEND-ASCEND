package br.com.ascend.the_system.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

@Service
public class PasswordService {

    private static final int SALT_LENGTH = 16;
    private static final int LEGACY_ITERATIONS = 120_000;
    private static final int CURRENT_ITERATIONS = 600_000;
    private static final int MAX_ITERATIONS = 2_000_000;
    private static final int KEY_LENGTH = 256;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    private final SecureRandom secureRandom = new SecureRandom();

    // Enable only after every instance can verify the iteration count in stored hashes.
    @Value("${ASCEND_UPGRADE_PASSWORD_HASHES:false}")
    private boolean upgradePasswordHashes;

    public String hash(String rawPassword) {
        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);
        int iterations = upgradePasswordHashes ? CURRENT_ITERATIONS : LEGACY_ITERATIONS;
        byte[] hash = pbkdf2(rawPassword.toCharArray(), salt, iterations);

        return iterations
                + ":"
                + Base64.getEncoder().encodeToString(salt)
                + ":"
                + Base64.getEncoder().encodeToString(hash);
    }

    public boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null) return false;
        try {
            String[] parts = storedHash.split(":", -1);
            if (parts.length != 3) return false;
            int iterations = Integer.parseInt(parts[0]);
            if (iterations < 1 || iterations > MAX_ITERATIONS) return false;
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[2]);
            if (salt.length != SALT_LENGTH || expectedHash.length != KEY_LENGTH / 8) return false;
            return constantTimeEquals(expectedHash, pbkdf2(rawPassword.toCharArray(), salt, iterations));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public boolean needsUpgrade(String storedHash) {
        if (!upgradePasswordHashes || storedHash == null) return false;
        try {
            return Integer.parseInt(storedHash.split(":", -1)[0]) < CURRENT_ITERATIONS;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private byte[] pbkdf2(char[] password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            try { return factory.generateSecret(spec).getEncoded(); }
            finally { spec.clearPassword(); java.util.Arrays.fill(password, '\0'); }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException exception) {
            throw new IllegalStateException("Não foi possível gerar hash da senha.", exception);
        }
    }

    private boolean constantTimeEquals(byte[] expected, byte[] actual) {
        if (expected.length != actual.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < expected.length; i++) {
            result |= expected[i] ^ actual[i];
        }

        return result == 0;
    }
}

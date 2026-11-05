package com.ait.pay.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Encrypts channel credentials before they are written to
 * {@code shop_pay_channel}.
 *
 * <p>Why bother, given the admin console is already behind auth: a database
 * backup, a read replica handed to an analyst, or a support engineer running a
 * {@code select *} would otherwise leak a live Stripe secret key. That is the
 * difference between "we had an incident" and "we rotated a key".
 *
 * <p>AES-GCM with a random 12-byte IV per value. The IV is prefixed to the
 * ciphertext and the whole thing is base64'd, so a column holds
 * {@code base64(iv || ciphertext || tag)} and is self-contained.
 *
 * <p>The master key comes from {@code ECOM_PAY_SECRET}. When it is absent the
 * codec degrades to a clearly marked passthrough so a fresh clone still boots —
 * and logs a warning every time it does, so nobody ships that by accident.
 */
@Slf4j
@Component
public class ChannelSecretCodec {

    /** Marks a value that is already encrypted, so re-saving does not double-wrap. */
    private static final String PREFIX = "enc:v1:";

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecureRandom random = new SecureRandom();
    private final SecretKeySpec key;
    private final boolean enabled;

    public ChannelSecretCodec(@Value("${ecom.pay.secret:}") String secret) {
        if (secret == null || secret.length() < 16) {
            this.key = null;
            this.enabled = false;
            log.warn("ECOM_PAY_SECRET is not set (or shorter than 16 chars): payment channel "
                    + "credentials will be stored in plain text. Set it before going live.");
        } else {
            // derive a 256-bit key by hashing whatever length the operator gave us
            this.key = new SecretKeySpec(sha256(secret), "AES");
            this.enabled = true;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Encrypts a value; null and blanks pass through untouched. */
    public String encode(String plain) {
        if (plain == null || plain.isEmpty()) {
            return plain;
        }
        if (!enabled) {
            return plain;
        }
        if (plain.startsWith(PREFIX)) {
            // already encrypted: the console re-submits the masked value
            return plain;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));

            byte[] packed = ByteBuffer.allocate(iv.length + encrypted.length)
                    .put(iv).put(encrypted).array();
            return PREFIX + Base64.getEncoder().encodeToString(packed);
        } catch (Exception ex) {
            // never let a crypto hiccup write plaintext by accident
            throw new IllegalStateException("failed to encrypt payment credential", ex);
        }
    }

    /** Decrypts a value; values without the marker are returned as-is. */
    public String decode(String stored) {
        if (stored == null || stored.isEmpty() || !stored.startsWith(PREFIX)) {
            return stored;
        }
        if (!enabled) {
            log.warn("cannot decrypt a payment credential because ECOM_PAY_SECRET is not set");
            return null;
        }
        try {
            byte[] packed = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            ByteBuffer buffer = ByteBuffer.wrap(packed);
            byte[] iv = new byte[IV_BYTES];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            // a wrong master key yields a tag mismatch, not an exception we can
            // recover from: log once and treat the channel as unconfigured
            log.error("failed to decrypt payment credential: {}", ex.getMessage());
            return null;
        }
    }

    /** True when the stored value looks encrypted. */
    public boolean looksEncrypted(String stored) {
        return stored != null && stored.startsWith(PREFIX);
    }

    private static byte[] sha256(String input) {
        try {
            return java.security.MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}

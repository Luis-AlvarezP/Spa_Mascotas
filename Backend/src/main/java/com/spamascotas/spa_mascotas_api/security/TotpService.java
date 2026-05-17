package com.spamascotas.spa_mascotas_api.security;

import org.apache.commons.codec.binary.Base32;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

@Component
public class TotpService {

    private static final int WINDOW = 1;

    public String generarSecret() {
        byte[] bytes = new byte[20];
        new SecureRandom().nextBytes(bytes);
        return new Base32().encodeToString(bytes);
    }

    public String urlQr(String correo, String secret) {
        String issuer = "SpaMascotas";
        String label = URLEncoder.encode(issuer + ":" + correo, StandardCharsets.UTF_8);
        return String.format("otpauth://totp/%s?secret=%s&issuer=%s", label, secret, issuer);
    }

    public boolean verificar(String secret, int code) {
        long timeStep = System.currentTimeMillis() / 1000L / 30L;
        for (int i = -WINDOW; i <= WINDOW; i++) {
            if (totp(secret, timeStep + i) == code) return true;
        }
        return false;
    }

    private int totp(String secret, long counter) {
        try {
            byte[] keyBytes = new Base32().decode(secret);
            byte[] msg = ByteBuffer.allocate(8).putLong(counter).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(keyBytes, "HmacSHA1"));
            byte[] hash = mac.doFinal(msg);
            int offset = hash[19] & 0x0F;
            return ((hash[offset] & 0x7F) << 24
                    | (hash[offset + 1] & 0xFF) << 16
                    | (hash[offset + 2] & 0xFF) << 8
                    | (hash[offset + 3] & 0xFF)) % 1_000_000;
        } catch (Exception e) {
            throw new RuntimeException("Error al generar TOTP", e);
        }
    }
}

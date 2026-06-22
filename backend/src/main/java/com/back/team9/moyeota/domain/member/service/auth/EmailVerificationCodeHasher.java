package com.back.team9.moyeota.domain.member.service.auth;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class EmailVerificationCodeHasher {

    private static final String ALGORITHM = "SHA-256";

    public String hash(String verificationCode) {
        return HexFormat.of().formatHex(
                digest(verificationCode)
        );
    }

    public boolean matches(
            String verificationCode,
            String verificationCodeHash
    ) {
        if (verificationCode == null
                || verificationCodeHash == null) {
            return false;
        }

        try {
            byte[] expectedHash = HexFormat.of()
                    .parseHex(verificationCodeHash);

            return MessageDigest.isEqual(
                    digest(verificationCode),
                    expectedHash
            );
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private byte[] digest(String value) {
        try {
            MessageDigest messageDigest =
                    MessageDigest.getInstance(ALGORITHM);

            return messageDigest.digest(
                    value.getBytes(StandardCharsets.UTF_8)
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 알고리즘을 사용할 수 없습니다.",
                    exception
            );
        }
    }
}
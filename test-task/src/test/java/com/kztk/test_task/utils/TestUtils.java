package com.kztk.test_task.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;

public class TestUtils {
    public static String generateValidHash(String initData, String botToken) throws Exception {
        String[] params = initData.split("&");
        Arrays.sort(params);

        String dataCheckString = String.join("\n", params);
        SecretKeySpec keySpec = new SecretKeySpec(
                MessageDigest.getInstance("SHA-256")
                        .digest(botToken.getBytes(StandardCharsets.UTF_8)),
                "HmacSHA256"
        );

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(keySpec);
        byte[] hash = mac.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8));

        return HexFormat.of().formatHex(hash);
    }
}

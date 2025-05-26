package com.kztk.test_task.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class TelegramDataValidator {
    @Value("${telegram.bot.token}")
    private String botToken;

    public boolean validate(String initData) {
        try {
            Map<String, String> params = parseInitData(initData);
            if (isExpired(params.get("auth_date"))) return false;
            return isValidHash(params);
        } catch (Exception e) {
            return false;
        }
    }

    // Превращает непонятную строку телеграма в мапу
    private Map<String, String> parseInitData(String initData) {
        return Arrays.stream(initData.split("&"))
                .map(p -> p.split("=", 2))
                .filter(p -> !p[0].equals("hash"))
                .collect(Collectors.toMap(
                        p -> p[0],
                        p -> URLDecoder.decode(p[1], StandardCharsets.UTF_8)
                ));
    }


    private boolean isExpired(String authDateStr) {
        long authDate = Long.parseLong(authDateStr);
        return (System.currentTimeMillis() / 1000 - authDate) > 86400;
    }

    /**
    1.Берёт все параметры кроме hash и сортирует их по алфавиту
    2.Собирает их в строку вида:
        auth_date=123456789
        first_name=John
        user={"id":123,...}
    3.Вычисляет секретный ключ из токена вашего бота
    4.Создаёт цифровую подпись для этой строки
    5.Сравнивает полученную подпись с той, что прислал Telegram
    */
    private boolean isValidHash(Map<String, String> params) throws NoSuchAlgorithmException, InvalidKeyException {
        String dataCheckString = params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("\n"));

        String secretKey = hmacSha256("WebAppData", botToken);
        String calculatedHash = hmacSha256(secretKey, dataCheckString);
        return calculatedHash.equals(params.get("hash"));
    }

    private String hmacSha256(String key, String data) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        byte[] hash = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    /**
     * Преобразует
     * Из компьютерного байтов: [12, -34, 5F]
     * В HEX: 0cde5f
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
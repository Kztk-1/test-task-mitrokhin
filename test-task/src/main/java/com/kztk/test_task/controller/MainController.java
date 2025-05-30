package com.kztk.test_task.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kztk.test_task.dto.TelegramUser;
import com.kztk.test_task.model.User;
import com.kztk.test_task.service.UserService;
import com.kztk.test_task.util.TelegramDataValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MainController {
    private final TelegramDataValidator validator;
    private final UserService userService;


    @GetMapping("/")
    public String home(
            @RequestParam(name = "initData", required = false) String initData,
            @RequestBody Map<String, Object> data,
            Model model
    ) {

        //NEW
        for (Map.Entry entry : data.entrySet()) {
            System.out.println(entry.getKey() + " " + entry.getValue());
        }

        if (initData == null || initData.isBlank()) {
            log.error("Missing initData parameter");
            model.addAttribute("errorMessage", "Invalid request: missing initData");
            return "error";
        }
        if (!validator.validate(initData)) {
            log.warn("Validation failed for initData: {}", initData);
            model.addAttribute("errorMessage", "Validation failed");
            return "error";
        }

        try {
            TelegramUser userData = parseUser(initData);
            log.info("Parsed user data: {}", userData);

            User user = userService.processUserData(userData);
            log.debug("Processed user: {}", user);

            model.addAttribute("user", user);
            return "home";
        } catch (IOException e) {
            log.error("Error parsing user data from initData: {}", initData, e);
            model.addAttribute("errorMessage", "Parsing failed");
            return "error";
        } catch (IllegalArgumentException e) {
            log.error("Invalid user data in initData: {}", initData, e);
            model.addAttribute("errorMessage", "Invalid user data");
            return "error";
        }
    }

    private TelegramUser parseUser(String initData) throws IOException {
        String[] params = initData.split("&");

        for (String param : params) {
            String[] pair = param.split("=", 2);
            if (pair.length == 2 && "user".equals(pair[0])) {
                String userJson = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                log.trace("Decoded user JSON: {}", userJson);

                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readValue(userJson, TelegramUser.class);
            }
        }
        log.error("User data not found in initData: {}", initData);
        throw new IllegalArgumentException("User data not found in initData");
    }
}
package com.kztk.test_task.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kztk.test_task.dto.TelegramUser;
import com.kztk.test_task.model.User;
import com.kztk.test_task.service.UserService;
import com.kztk.test_task.util.TelegramDataValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequiredArgsConstructor
public class MainController {
    private final TelegramDataValidator validator;
    private final UserService userService;

    /*
    интерфейс Model (пакет org.springframework.ui) — это просто «контейнер»
    (по сути, расширяемый Map<String, Object>), который используется для передачи
    данных из контроллера в представление (view).

    Когда Spring видит в сигнатуре метода параметр типа Model (или ModelMap, или Map<String,Object>),
    он автоматически подставляет туда свою реализацию этого интерфейса.
    Он содержит коллекцию пар «ключ–значение» (атрибуты) для передачи во view.

    после return "home" все эти атрибуты становятся доступными в вашем шаблоне
    под теми именами, что вы указали.
    */
    @GetMapping("/")
    public String home(@RequestParam String initData, Model model) {
        if (!validator.validate(initData)) {
            return "error";
        }

        TelegramUser userData;
        try {
            userData = parseUser(initData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        User user = userService.processUserData(userData); // Обрабатываем и возвращаем обновленное/добавленное
        model.addAttribute("user", user);
        return "home"; // Шаблон home.html, где можно обращаться к ${user}
    }

    private TelegramUser parseUser(String initData) throws IOException {
        // Разбиваем параметры initData
        /*
        1)
        user.id=123456789
        &user.is_bot=false
        &user.first_name=%D0%98%D0%B2%D0%B0%D0%BD
        &user.last_name=%D0%98%D0%B2%D0%B0%D0%BD%D0%BE%D0%B2
        ...
         */
        String[] params = initData.split("&");
        /*
        2)
        user.id=123456789, user.is_bot=false, user.first_name=%D0%9..., user.last_name=%D0...
        */
        for (String param : params) {
            String[] pair = param.split("=", 2);
            /*
            3)
            user.id=123456789
            */
            if (pair.length == 2 && "user".equals(pair[0])) {
                /*
                Декодируем URL-encoded JSON
                До декодирования: "%7B%22id%22%3A123456789%2C%22isBot%22%3Afalse%2C%22firstName%22%3A%..."
                После декодирования: {"id":123456789,"isBot":false,"firstName":"Иван"}
                */
                String userJson = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);

                // Парсим JSON в объект
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readValue(userJson, TelegramUser.class);
            }
        }
        throw new IllegalArgumentException("User data error");
    }
}
package com.kztk.test_task.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kztk.test_task.dto.TelegramUser;
import com.kztk.test_task.model.User;
import com.kztk.test_task.repository.UserRepository;
import com.kztk.test_task.util.TelegramDataValidator;
import com.kztk.test_task.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "telegram.bot.token=test_token",
        "spring.jpa.hibernate.ddl-auto=create-drop",
})
class MainControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String validHash = "generated_valid_hash";
    private final String invalidHash = "invalid_hash";
    private TelegramUser testUser;

    @BeforeEach
    void setup() {
        // Очистка бд
        userRepository.deleteAll();

        testUser = new TelegramUser();
        testUser.setId(123L);
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setUsername("johndoe");
    }


    @Test
    void shouldSaveNewUserToDatabase() throws Exception {
        String userJson = """
        {
            "id": 123,
            "username": "johndoe",
            "first_name": "John",
            "last_name": "Doe"
        }
        """;
        String encodedUser = URLEncoder.encode(userJson, StandardCharsets.UTF_8);

        String authDate = String.valueOf(System.currentTimeMillis() / 1000);
        String initData = "auth_date=" + authDate + "&user=" + encodedUser;

        // Сортировка параметров по алфавиту
        String[] params = initData.split("&");
        Arrays.sort(params);
        String sortedData = String.join("&", params);

        String validHash = TestUtils.generateValidHash(sortedData, "test_token");
        String fullInitData = sortedData + "&hash=" + validHash;

        mockMvc.perform(get("/")
                        .param("initData", fullInitData))
                .andExpect(status().isOk())
                .andExpect(view().name("home"));
    }


    @Test
    void shouldUpdateExistingUserInDatabase() throws Exception {
        // Initial save
        User existingUser = new User();
        existingUser.setTelegramId(testUser.getId());
        existingUser.setFirstName("Old Name");
        userRepository.save(existingUser);

        // Update data
        testUser.setFirstName("New Name");

        String userJson = objectMapper.writeValueAsString(testUser);
        String encodedUser = URLEncoder.encode(userJson, StandardCharsets.UTF_8);
        String authDate = String.valueOf(System.currentTimeMillis() / 1000);
        String initData = "auth_date=" + authDate + "&user=" + encodedUser;

        // Сортировка параметров
        String[] params = initData.split("&");
        Arrays.sort(params);
        String sortedData = String.join("&", params);

        String validHash = TestUtils.generateValidHash(sortedData, "test_token");
        String fullInitData = sortedData + "&hash=" + validHash;

        mockMvc.perform(get("/").param("initData", fullInitData))
                .andExpect(status().isOk());

        User updatedUser = userRepository.findById(testUser.getId()).get();
        assertEquals("New Name", updatedUser.getFirstName());
        assertNotNull(updatedUser.getUpdatedAt());
    }

    @Test
    void shouldNotSaveUserWithInvalidData() throws Exception {
        String userJson = objectMapper.writeValueAsString(testUser);
        String encodedUser = URLEncoder.encode(userJson, StandardCharsets.UTF_8);
        String authDate = String.valueOf(System.currentTimeMillis() / 1000);
        String initData = "auth_date=" + authDate + "&user=" + encodedUser;

        // Сортировка и генерация с неверным токеном
        String[] params = initData.split("&");
        Arrays.sort(params);
        String sortedData = String.join("&", params);

        String invalidHash = TestUtils.generateValidHash(sortedData, "wrong_token");
        String fullInitData = sortedData + "&hash=" + invalidHash;

        mockMvc.perform(get("/").param("initData", fullInitData))
                .andExpect(view().name("error"));

        Optional<User> user = userRepository.findById(testUser.getId());
        assertTrue(user.isEmpty());
    }

    private String buildInitData(TelegramUser user, String hash) throws Exception {
        String userJson = objectMapper.writeValueAsString(user);
        String authDate = String.valueOf(System.currentTimeMillis() / 1000); // Текущее время

        // Сортировка параметров
        String authDateParam = "auth_date=" + authDate;
        String userParam = "user=" + URLEncoder.encode(userJson, StandardCharsets.UTF_8);

        return authDateParam + "&" + userParam + "&hash=" + hash;
    }
}
package com.kztk.test_task.service;

import com.kztk.test_task.dto.TelegramUser;
import com.kztk.test_task.model.User;
import com.kztk.test_task.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // Либо обновляем старого User, либо добавляем нового
    public User processUserData(TelegramUser userData) {
        User user = userRepository.findById(userData.getId())
                .orElseGet(User::new);

        user.setTelegramId(user.getTelegramId());
        user.setFirstName(userData.getFirstName());
        user.setLastName(userData.getLastName());
        user.setUsername(userData.getUsername());

        if (user.getCreatedAt() == null) user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

}
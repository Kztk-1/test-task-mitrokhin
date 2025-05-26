package com.kztk.test_task.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;


@Data
@Entity
@Table(name = "users")
public class User {
    @Id
    private Long telegramId;
    private String firstName;
    private String lastName;
    private String username;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
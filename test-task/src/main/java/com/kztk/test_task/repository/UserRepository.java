package com.kztk.test_task.repository;

import com.kztk.test_task.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
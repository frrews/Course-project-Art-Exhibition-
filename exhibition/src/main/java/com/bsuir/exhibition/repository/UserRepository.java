package com.bsuir.exhibition.repository;

import com.bsuir.exhibition.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUsername(String username);
    Optional<User> findUserByEmail(String email);
}
package com.example.userservice.service;

import com.example.userservice.entity.User;
import com.example.userservice.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;

    @Value("${user.default-rating}")
    private Integer defaultRating;

    public User save(User user) {

        if (userRepo.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        if (userRepo.existsByEmailId(user.getEmailId())) {
            throw new RuntimeException("Email already exists");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        if (user.getRating() == null) {
            user.setRating(defaultRating);
        }

        return userRepo.save(user);
    }
}

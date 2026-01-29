package com.example.userservice.service;

import com.example.userservice.entity.User;
import com.example.userservice.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepo userRepo;

    public User getUserByUsername(String username) {
        return userRepo.findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));
    }

    public User getUserByEmail(String email) {
        return userRepo.findByEmailId(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));
    }
}

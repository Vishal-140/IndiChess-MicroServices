package com.example.userservice.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    @GetMapping("/user/username")
    public String getUser(Authentication authentication) {
        return authentication.getName();
    }


}

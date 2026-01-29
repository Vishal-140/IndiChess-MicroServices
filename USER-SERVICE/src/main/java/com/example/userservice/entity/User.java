package com.example.userservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Size(min = 4, max = 50, message = "Username must be between 4 and 50 characters")
    @Column(name = "user_name", unique = true, nullable = false)
    private String username;

    @Email
    @Column(name = "email_id", unique = true, nullable = false)
    private String emailId;

    @Size(min = 6, max = 512)
    @Column(nullable = false)
    private String password;

    private String pfpUrl;

    private String country;

    private Integer rating;
}


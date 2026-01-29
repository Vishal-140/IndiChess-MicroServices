package com.example.gameservice.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(
        name = "moves",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"game_id", "ply"}
        )
)
public class Move {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // reference only by ID
    @Column(name = "game_id", nullable = false)
    private Long gameId;

    @Column(nullable = false)
    private Integer ply;        // half-move: 1,2,3...

    @Column(nullable = false)
    private Integer moveNumber; // full move: 1,2,3...

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PieceColor color;   // WHITE / BLACK

    @Column(length = 10)
    private String uci;

    @Column(length = 10)
    private String san;

    @Column(length = 200)
    private String fenBefore;

    @Column(length = 200)
    private String fenAfter;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}

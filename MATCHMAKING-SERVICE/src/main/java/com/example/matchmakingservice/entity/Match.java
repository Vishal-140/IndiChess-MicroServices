package com.example.matchmakingservice.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "matches")
@Data
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player1_id", nullable = false)
    private Long player1Id;

    @Column(name = "player2_id", nullable = false)
    private Long player2Id;

    @Column(name = "game_id")
    private Long gameId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameType gameType;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Match() {}

    public Match(Long player1Id, Long player2Id, MatchStatus status, GameType gameType) {
        this.player1Id = player1Id;
        this.player2Id = player2Id;
        this.status = status;
        this.gameType = gameType;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}

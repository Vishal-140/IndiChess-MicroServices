package com.example.gameservice.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "games")
@Data
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // White player
    @Column(name = "player1_id", nullable = false)
    private Long player1Id;

    // Black player
    @Column(name = "player2_id", nullable = false)
    private Long player2Id;

    // Link to Match
    @Column(name = "match_id", nullable = false)
    private Long matchId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameStatus status;

    @Column(nullable = false)
    private Integer currentPly;

    @Column(name = "fen_current", length = 200)
    private String fenCurrent;

    @Column(name = "last_move_uci", length = 10)
    private String lastMoveUci;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameType gameType;

    private Integer whiteTime;
    private Integer blackTime;

    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private static final String START_FEN =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    // Used for MANUAL game creation
    public Game(Long player1Id, Long player2Id, GameType gameType) {
        this.player1Id = player1Id;
        this.player2Id = player2Id;
        this.gameType = gameType;
        this.status = GameStatus.IN_PROGRESS;
        this.currentPly = 0;
        this.fenCurrent = START_FEN;
        this.startedAt = LocalDateTime.now();

        if (gameType == GameType.BLITZ) {
            this.whiteTime = 180;
            this.blackTime = 180;
        } else if (gameType == GameType.RAPID) {
            this.whiteTime = 600;
            this.blackTime = 600;
        }
    }

    public Game() {}

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.startedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

package com.example.gameservice.controller;

import com.example.gameservice.dto.GameResponse;
import com.example.gameservice.dto.MoveRequest;
import com.example.gameservice.dto.MoveResponse;
import com.example.gameservice.entity.Game;
import com.example.gameservice.entity.GameType;
import com.example.gameservice.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    // =================================================
    // USED BY MATCHMAKING (OPEN FEIGN)
    // =================================================
    @PostMapping("/game/create")
    public Long createGameForMatchmaking(
            @RequestParam Long whitePlayerId,
            @RequestParam Long blackPlayerId,
            @RequestParam Long matchId,
            @RequestParam GameType gameType
    ) {
        Game game = gameService.createGameFromMatchmaking(
                whitePlayerId,
                blackPlayerId,
                matchId,
                gameType
        );
        return game.getId();
    }

    // =========================
    // CREATE GAME
    // =========================
    @PostMapping("/games")
    public GameResponse createGame(
            @RequestHeader("X-PLAYER1-ID") Long player1Id,
            @RequestHeader("X-PLAYER2-ID") Long player2Id,
            @RequestParam GameType gameType
    ) {
        Game game =
                gameService.createGame(player1Id, player2Id, gameType);

        return GameResponse.from(game);
    }

    // =========================
    // GET GAME
    // =========================
    @GetMapping("/games/{gameId}")
    public GameResponse getGame(
            @PathVariable Long gameId,
            @RequestHeader("X-USER-ID") Long userId
    ) {
        return gameService.getGameDetails(gameId, userId);
    }

    // =========================
    // MAKE MOVE
    // =========================
    @PostMapping("/games/{gameId}/move")
    public MoveResponse makeMove(
            @PathVariable Long gameId,
            @RequestHeader("X-USER-ID") Long userId,
            @RequestBody MoveRequest moveRequest
    ) {
        return gameService.makeMove(gameId, userId, moveRequest);
    }
}

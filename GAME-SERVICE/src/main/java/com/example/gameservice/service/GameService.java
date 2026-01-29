package com.example.gameservice.service;

import com.example.gameservice.dto.MoveRequest;
import com.example.gameservice.dto.MoveResponse;
import com.example.gameservice.entity.*;
import com.example.gameservice.repo.GameRepo;
import com.example.gameservice.repo.MoveRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepo gameRepo;
    private final MoveRepo moveRepo;

    // =========================
    // CREATE GAME
    // =========================
    public Game createGame(
            Long player1Id,
            Long player2Id,
            GameType gameType
    ) {
        Game game = new Game(player1Id, player2Id, gameType);
        game.setStatus(GameStatus.IN_PROGRESS);
        gameRepo.save(game);
        return game;
    }

    // =========================
    // GET GAME
    // =========================
    public Game getGame(Long gameId, Long userId) {

        Game game = gameRepo.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));

        if (!userId.equals(game.getPlayer1Id())
                && !userId.equals(game.getPlayer2Id())) {
            throw new RuntimeException("Not your game");
        }

        return game;
    }

    // =========================
    // MAKE MOVE
    // =========================
    public MoveResponse makeMove(
            Long gameId,
            Long userId,
            MoveRequest request
    ) {

        Game game = gameRepo.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));

        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            throw new RuntimeException("Game already finished");
        }

        boolean isWhiteTurn = game.getCurrentPly() % 2 == 0;
        Long expectedPlayer =
                isWhiteTurn ? game.getPlayer1Id() : game.getPlayer2Id();

        if (!userId.equals(expectedPlayer)) {
            throw new RuntimeException("Not your turn");
        }

        // SAVE MOVE
        Move move = new Move();
        move.setGameId(gameId);
        move.setPly(game.getCurrentPly() + 1);
        move.setMoveNumber((move.getPly() + 1) / 2);
        move.setColor(isWhiteTurn ? PieceColor.WHITE : PieceColor.BLACK);
        move.setUci(request.getUci());
        move.setFenBefore(game.getFenCurrent());
        move.setFenAfter(game.getFenCurrent()); // engine later

        moveRepo.save(move);

        // UPDATE GAME
        game.setCurrentPly(move.getPly());
        game.setLastMoveUci(move.getUci());
        gameRepo.save(game);

        // RESPONSE
        MoveResponse res = new MoveResponse();
        res.setGameId(gameId);
        res.setUci(move.getUci());
        res.setFen(game.getFenCurrent());
        res.setCurrentPly(game.getCurrentPly());
        res.setWhiteTime(game.getWhiteTime());
        res.setBlackTime(game.getBlackTime());
        res.setNextTurn(isWhiteTurn ? "BLACK" : "WHITE");
        res.setStatus(game.getStatus().name());

        return res;
    }

    // =========================
    // RESIGN
    // =========================
    public void resign(Long gameId, Long userId) {

        Game game = gameRepo.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));

        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            throw new RuntimeException("Game already finished");
        }

        if (userId.equals(game.getPlayer1Id())) {
            game.setStatus(GameStatus.PLAYER2_WON);
        } else if (userId.equals(game.getPlayer2Id())) {
            game.setStatus(GameStatus.PLAYER1_WON);
        } else {
            throw new RuntimeException("Not your game");
        }

        game.setFinishedAt(LocalDateTime.now());
        gameRepo.save(game);
    }
}

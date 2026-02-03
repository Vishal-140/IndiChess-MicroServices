package com.example.gameservice.service;

import com.example.gameservice.dto.MoveRequest;
import com.example.gameservice.dto.MoveResponse;
import com.example.gameservice.entity.*;
import com.example.gameservice.repo.GameRepo;
import com.example.gameservice.repo.MoveRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepo gameRepo;
    private final MoveRepo moveRepo;
    private final SimpMessagingTemplate messagingTemplate;

    // =================================================
    // CALLED BY MATCHMAKING (FEIGN)
    // =================================================
    public Game createGameFromMatchmaking(
            Long whitePlayerId,
            Long blackPlayerId,
            Long matchId,
            GameType gameType
    ) {
        Game game = new Game(whitePlayerId, blackPlayerId, gameType);
        game.setMatchId(matchId);
        game.setStatus(GameStatus.IN_PROGRESS);
        game.setCurrentPly(0);
        game.setFenCurrent(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        );

        return gameRepo.save(game);
    }

    // =========================
    // CREATE GAME (MANUAL)
    // =========================
    public Game createGame(
            Long player1Id,
            Long player2Id,
            GameType gameType
    ) {
        Game game = new Game(player1Id, player2Id, gameType);
        game.setStatus(GameStatus.IN_PROGRESS);
        game.setCurrentPly(0);
        return gameRepo.save(game);
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

        System.out.println("Processing Move: userId=" + userId 
             + ", gameId=" + gameId 
             + ", ply=" + game.getCurrentPly() 
             + ", isWhiteTurn=" + isWhiteTurn 
             + ", expectedPlayer=" + expectedPlayer);

        if (!userId.equals(expectedPlayer)) {
            System.err.println("Move REJECTED: Not user's turn.");
            throw new RuntimeException("Not your turn");
        }

        Move move = new Move();
        move.setGameId(gameId);
        move.setPly(game.getCurrentPly() + 1);
        move.setMoveNumber((move.getPly() + 1) / 2);
        move.setColor(isWhiteTurn ? PieceColor.WHITE : PieceColor.BLACK);
        move.setUci(request.getUci());
        move.setFenBefore(game.getFenCurrent());
        String newFen = com.example.gameservice.logic.GameEngine.applyMove(game.getFenCurrent(), request.getUci());
        move.setFenAfter(newFen);

        moveRepo.save(move);

        game.setCurrentPly(move.getPly());
        game.setFenCurrent(newFen);
        game.setLastMoveUci(move.getUci());
        
        // Update Game Status (Checkmate/Stalemate)
        GameStatus status = com.example.gameservice.logic.GameEngine.getGameStatus(newFen);
        game.setStatus(status);
        if (status != GameStatus.IN_PROGRESS) {
            game.setFinishedAt(LocalDateTime.now());
        }
        
        gameRepo.save(game);

        MoveResponse res = new MoveResponse();
        res.setGameId(gameId);
        res.setUci(move.getUci());
        res.setFen(game.getFenCurrent());
        res.setCurrentPly(game.getCurrentPly());
        res.setWhiteTime(game.getWhiteTime());
        res.setBlackTime(game.getBlackTime());
        res.setNextTurn(isWhiteTurn ? "BLACK" : "WHITE");
        res.setStatus(game.getStatus().name());

        // Broadcast move to subscribers
        messagingTemplate.convertAndSend("/topic/game/" + gameId, res);

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
            game.setStatus(GameStatus.BLACK_WON);
        } else if (userId.equals(game.getPlayer2Id())) {
            game.setStatus(GameStatus.WHITE_WON);
        } else {
            throw new RuntimeException("Not your game");
        }

        game.setFinishedAt(LocalDateTime.now());
        gameRepo.save(game);
    }
}

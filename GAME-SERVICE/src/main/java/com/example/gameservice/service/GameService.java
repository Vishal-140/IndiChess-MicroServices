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
        
        // Initialize Timers (in seconds)
        int initialTime = getInitialTimeInSeconds(gameType);
        game.setWhiteTime(initialTime);
        game.setBlackTime(initialTime);
        game.setLastMoveTimestamp(LocalDateTime.now()); // Set start time

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
        game.setMatchId(0L); // Set default matchId for manual/direct games
        
        // Initialize Timers
        int initialTime = getInitialTimeInSeconds(gameType);
        game.setWhiteTime(initialTime);
        game.setBlackTime(initialTime);
        game.setLastMoveTimestamp(LocalDateTime.now());

        return gameRepo.save(game);
    }

    private int getInitialTimeInSeconds(GameType gameType) {
        switch (gameType) {
            case BLITZ: return 180; // 3 mins
            case RAPID: return 600;  // 10 mins
            case STANDARD: return 3600; // 60 mins
            default: return 600;
        }
    }

    // =========================
    // GET GAME
    // =========================
    public com.example.gameservice.dto.GameResponse getGameDetails(Long gameId, Long userId) {
        Game game = gameRepo.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));

        System.out.println("GET GAME DETAILS: gameId=" + gameId + ", P1=" + game.getPlayer1Id() + ", P2=" + game.getPlayer2Id() + ", reqUser=" + userId);

        if (!userId.equals(game.getPlayer1Id())
                && !userId.equals(game.getPlayer2Id())) {
            throw new RuntimeException("Not your game");
        }
        
        com.example.gameservice.dto.GameResponse response = com.example.gameservice.dto.GameResponse.from(game);
        
        java.util.List<Move> moves = moveRepo.findByGameIdOrderByPlyAsc(gameId);
        java.util.List<MoveResponse> moveResponses = moves.stream().map(move -> {
            MoveResponse mr = new MoveResponse();
            mr.setGameId(move.getGameId());
            mr.setUci(move.getUci());
            mr.setSan(move.getSan() != null ? move.getSan() : move.getUci()); // Fallback to UCI
            mr.setFen(move.getFenAfter());
            mr.setCurrentPly(move.getPly());
            // mr.setNextTurn, etc. can be inferred or left null if not critical for history
            return mr;
        }).collect(java.util.stream.Collectors.toList());
        
        response.setMoves(moveResponses);
        return response;
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
        
        // --- TIME MANAGEMENT ---
        if (game.getGameType() != GameType.STANDARD) { // Assuming STANDARD has no timer or handled differently
            long elapsedSeconds = java.time.Duration.between(game.getLastMoveTimestamp(), LocalDateTime.now()).toSeconds();
            
            // Note: The one who JUST moved (userId) was thinking. Subtract from them.
            if (userId.equals(game.getPlayer1Id())) { // White moved
                int newTime = (int) (game.getWhiteTime() - elapsedSeconds);
                if (game.getGameType() == GameType.BLITZ) newTime += 2; // Increment 3|2
                game.setWhiteTime(Math.max(0, newTime));

                if (newTime <= 0) {
                     game.setStatus(GameStatus.BLACK_WON); // White flag fall
                     System.out.println("TIMEOUT: White ran out of time");
                }
            } else { // Black moved
                int newTime = (int) (game.getBlackTime() - elapsedSeconds);
                if (game.getGameType() == GameType.BLITZ) newTime += 2; // Increment
                game.setBlackTime(Math.max(0, newTime));

                if (newTime <= 0) {
                    game.setStatus(GameStatus.WHITE_WON); // Black flag fall
                    System.out.println("TIMEOUT: Black ran out of time");
                }
            }
            game.setLastMoveTimestamp(LocalDateTime.now());
        }
        
        // --- DRAW CHECKS ---
        // Fetch history for 3-fold repetition
        // Optimize: we only strictly need list of FENs.
        // We can limit fetch if performance is concern, but for chess game (<200 moves) it's fine.
        java.util.List<Move> history = moveRepo.findByGameIdOrderByPlyAsc(gameId);
        // We need list of Strings (FENs)
        java.util.List<String> historyFens = history.stream()
                .map(Move::getFenAfter) // Use post-move FENs to check repetition of resulting position
                .collect(java.util.stream.Collectors.toList());
        // Also add the START_FEN if it's not in moves?
        // Actually Repetition checks positions after moves. 
        // Initial position is relevant too. Let's add Start FEN if Ply 0.
        // But simpler: just use move history for now as most repetitions happen mid-game. 
        // Ideally we should include "fenBefore" of first move or just "game start fen".
        // Let's add basic start fen if needed.
        // For standard game:
        historyFens.add(0, "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");

        // Update Game Status (Checkmate/Stalemate/Draws)
        GameStatus status = com.example.gameservice.logic.GameEngine.getGameStatus(newFen, historyFens);
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

        // Broadcast Resignation
        MoveResponse res = new MoveResponse();
        res.setGameId(gameId);
        res.setFen(game.getFenCurrent()); // No change in FEN usually, or maybe update it?
        res.setStatus(game.getStatus().name());
        res.setNextTurn("NONE");
        
        messagingTemplate.convertAndSend("/topic/game/" + gameId, res);
    }

    // =========================
    // DRAW OFFER
    // =========================
    public void offerDraw(Long gameId, Long userId) {
        // Validate game exists and user is part of it
        Game game = gameRepo.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));
        
        if (game.getStatus() != GameStatus.IN_PROGRESS) return;

        // Broadcast Draw Offer
        MoveResponse res = new MoveResponse();
        res.setGameId(gameId);
        res.setFen(game.getFenCurrent()); 
        res.setStatus(game.getStatus().name());
        res.setDrawOfferBy(String.valueOf(userId)); // Signal that this user offered draw
        
        messagingTemplate.convertAndSend("/topic/game/" + gameId, res);
    }

    public void respondDraw(Long gameId, Long userId, boolean accept) {
         Game game = gameRepo.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));
         
         if (game.getStatus() != GameStatus.IN_PROGRESS) return;

         if (accept) {
             game.setStatus(GameStatus.DRAW);
             game.setFinishedAt(LocalDateTime.now());
             gameRepo.save(game);
             
             MoveResponse res = new MoveResponse();
             res.setGameId(gameId);
             res.setFen(game.getFenCurrent());
             res.setStatus("DRAW"); // Or GameStatus.DRAW.name()
             res.setDrawOfferBy(null); // Clear offer
             
             messagingTemplate.convertAndSend("/topic/game/" + gameId, res);
         } else {
             // If rejected, maybe just notify? 
             // Or simply clear the offer state on client by sending drawOfferBy = "REJECTED" or null?
             // Let's send null or "REJECTED"
             MoveResponse res = new MoveResponse();
             res.setGameId(gameId);
             res.setFen(game.getFenCurrent());
             res.setStatus(game.getStatus().name());
             res.setDrawOfferBy("REJECTED"); // Client interprets this to close modal
             
             messagingTemplate.convertAndSend("/topic/game/" + gameId, res);
         }
    }
}

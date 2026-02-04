package com.example.gameservice.scheduler;

import com.example.gameservice.entity.Game;
import com.example.gameservice.entity.GameStatus;
import com.example.gameservice.entity.GameType;
import com.example.gameservice.repo.GameRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.example.gameservice.dto.MoveResponse;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GameTimeoutScheduler {

    private final GameRepo gameRepo;
    private final SimpMessagingTemplate messagingTemplate;

    @Scheduled(fixedRate = 2000) // Check every 2 seconds
    public void checkForTimeouts() {
        // Find all games In Progress (Naive implementation: fetch all. In prod, use paginated query or status-based index)
        List<Game> activeGames = gameRepo.findByStatus(GameStatus.IN_PROGRESS);

        for (Game game : activeGames) {
            if (game.getGameType() == GameType.STANDARD) continue; // No timer for Standard

            long elapsedSeconds = java.time.Duration.between(game.getLastMoveTimestamp(), LocalDateTime.now()).toSeconds();
            boolean isWhiteTurn = game.getCurrentPly() % 2 == 0;
            boolean isTimeout = false;

            if (isWhiteTurn) {
                if (elapsedSeconds >= game.getWhiteTime()) {
                    // White lost on time
                    game.setStatus(GameStatus.BLACK_WON);
                    game.setWhiteTime(0);
                    isTimeout = true;
                }
            } else {
                if (elapsedSeconds >= game.getBlackTime()) {
                    // Black lost on time
                    game.setStatus(GameStatus.WHITE_WON);
                    game.setBlackTime(0);
                    isTimeout = true;
                }
            }

            if (isTimeout) {
                game.setFinishedAt(LocalDateTime.now());
                gameRepo.save(game);
                
                // Broadcast Timeout
                MoveResponse res = new MoveResponse();
                res.setGameId(game.getId());
                res.setFen(game.getFenCurrent());
                res.setStatus(game.getStatus().name());
                res.setNextTurn("NONE");
                res.setWhiteTime(game.getWhiteTime());
                res.setBlackTime(game.getBlackTime());
                
                messagingTemplate.convertAndSend("/topic/game/" + game.getId(), res);
                System.out.println("SCHEDULER: Game " + game.getId() + " ended due to TIMEOUT.");
            }
        }
    }
}

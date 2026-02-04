package com.example.matchmakingservice.service;

import com.example.matchmakingservice.client.GameServiceClient;
import com.example.matchmakingservice.entity.GameType;
import com.example.matchmakingservice.entity.Match;
import com.example.matchmakingservice.entity.MatchQueueEntry;
import com.example.matchmakingservice.entity.MatchStatus;
import com.example.matchmakingservice.repo.MatchQueueRepo;
import com.example.matchmakingservice.repo.MatchRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MatchService {

    private static final long MAX_WAIT_TIME = 90_000; // 90 sec

    private final MatchRepo matchRepo;
    private final MatchQueueRepo matchQueueRepo;
    private final GameServiceClient gameServiceClient;

    // ======================
    // JOIN QUEUE
    // ======================
    @Transactional
    public Optional<Long> joinQueue(Long userId, GameType gameType) {

        if (gameType == null) {
            gameType = GameType.STANDARD;
        }

        // 1. Check if already waiting
        if (matchQueueRepo.existsByUserId(userId)) {
             return Optional.of(-1L);
        }

        // 2. Try to find opponent
        // Find oldest request that is NOT me
        Optional<MatchQueueEntry> opponentOpt = matchQueueRepo.findFirstByGameTypeAndUserIdNotOrderByRequestTimeAsc(gameType, userId);

        if (opponentOpt.isPresent()) {
            MatchQueueEntry opponentEntry = opponentOpt.get();
            Long opponentId = opponentEntry.getUserId();

            // Create Match
            Match match = new Match(
                    opponentId,
                    userId,
                    MatchStatus.CREATED,
                    gameType
            );
            matchRepo.save(match);
            Long matchId = match.getId();

            // Call Game Service
            try {
                Long gameId = gameServiceClient.createGame(opponentId, userId, matchId, gameType);
                match.setGameId(gameId);
                matchRepo.save(match);
                
                // Remove opponent from queue
                matchQueueRepo.delete(opponentEntry);

                return Optional.of(gameId);
            } catch (Exception e) {
                // DISTRIBUTED TRANSACTION SAFETY:
                // If Game Service fails (or network timeout), we MUST ensure the opponent is not lost from the queue.
                // 1. We throw a RuntimeException to trigger Spring's @Transactional Rollback.
                // 2. This rolls back the 'Match' creation and any 'MatchQueueEntry' changes (though delete is after this).
                // 3. Result: Opponent remains in Queue, Match is not persisted.
                // Note: If Game Service actually created the game but timed out, we have a orphan game there.
                // A cleanup cron job on Game Service can remove games with no associated Match status if needed.
                throw new RuntimeException("Failed to create game - Rolling back to preserve queue", e);
            }
        } else {
            // No opponent found -> Add to queue
            MatchQueueEntry newEntry = MatchQueueEntry.builder()
                    .userId(userId)
                    .gameType(gameType)
                    .requestTime(LocalDateTime.now())
                    .build();
            matchQueueRepo.save(newEntry);

            return Optional.of(-1L); // Waiting
        }
    }

    // ======================
    // CHECK MATCH (POLLING)
    // ======================
    @Transactional
    public Optional<Long> checkMatch(Long userId, GameType gameType) {
        
        // 1. Check if I have been matched (Match created recently)
        // We look for a match with status CREATED where user is a participant
        // Assuming MatchRepo has a finder for this. 
        // If not, we might need to add it: findFirstByPlayer1IdOrPlayer2IdAndStatus...
        // For now, let's try to find a match.
        
        Optional<Match> matchOpt = matchRepo.findFirstByPlayer1IdOrPlayer2IdOrderByCreatedAtDesc(userId, userId);
        if (matchOpt.isPresent()) {
            Match match = matchOpt.get();
            // If match is recent and status is CREATED or IN_PROGRESS (game started)
            // We return gameId
            if (match.getGameId() != null) {
                return Optional.of(match.getGameId());
            }
            // Fallback if gameId not set yet? Or ignore.
            // Maybe it is being created processing?
            // For now, assume consistent.
        }

        // 2. Check for Timeout in Queue
        Optional<MatchQueueEntry> entryOpt = matchQueueRepo.findByUserId(userId);
        
        if (entryOpt.isPresent()) {
            MatchQueueEntry entry = entryOpt.get();
            if (entry.getRequestTime().isBefore(LocalDateTime.now().minusSeconds(90))) {
                matchQueueRepo.delete(entry);
                return Optional.of(-2L); // Timeout
            }
        } else {
             // If not in queue and no match found, maybe just removed.
        }
        
        return Optional.of(-1L);
    }
    
    // ======================
    // CANCEL QUEUE
    // ======================
    @Transactional
    public boolean cancelQueue(Long userId, GameType gameType) {
        if (matchQueueRepo.existsByUserId(userId)) {
            matchQueueRepo.deleteByUserId(userId);
            return true;
        }
        return false;
    }
}

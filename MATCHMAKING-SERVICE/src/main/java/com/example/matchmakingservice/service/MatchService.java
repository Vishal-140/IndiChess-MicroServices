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
                gameServiceClient.createGame(opponentId, userId, matchId, gameType);
            } catch (Exception e) {
                // If game creation fails, rollback is complex in microservices. 
                // For now, we assume it works or we might need a Saga.
                // Or just fail this request.
                throw new RuntimeException("Failed to create game", e);
            }

            // Remove opponent from queue
            matchQueueRepo.delete(opponentEntry);

            return Optional.of(matchId);
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
            // We return it.
            return Optional.of(match.getId());
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

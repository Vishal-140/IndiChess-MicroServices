package com.example.matchmakingservice.service;

import com.example.matchmakingservice.entity.GameType;
import com.example.matchmakingservice.entity.Match;
import com.example.matchmakingservice.entity.MatchStatus;
import com.example.matchmakingservice.repo.MatchRepo;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MatchService {

    // waiting players per game type
    private final Map<GameType, Set<Long>> waitingPlayers =
            new ConcurrentHashMap<>();

    // userId -> wait start time
    private final Map<Long, Long> waitingStartTime =
            new ConcurrentHashMap<>();

    private static final long MAX_WAIT_TIME = 90_000; // 90 sec

    private final MatchRepo matchRepo;
    private final MatchQueueService matchQueueService;

    public MatchService(
            MatchRepo matchRepo,
            MatchQueueService matchQueueService
    ) {
        this.matchRepo = matchRepo;
        this.matchQueueService = matchQueueService;

        for (GameType type : GameType.values()) {
            waitingPlayers.put(type, ConcurrentHashMap.newKeySet());
        }
    }

    // JOIN QUEUE
    public Optional<Long> joinQueue(Long userId, GameType gameType) {

        if (gameType == null) gameType = GameType.STANDARD;

        Set<Long> queue = waitingPlayers.get(gameType);

        synchronized (queue) {

            if (queue.contains(userId)) {
                return Optional.of(-1L); // already waiting
            }

            for (Long opponentId : queue) {

                if (!opponentId.equals(userId)) {

                    Match match = new Match(
                            opponentId,
                            userId,
                            MatchStatus.CREATED, // ✅ FIX
                            gameType
                    );

                    matchRepo.save(match);
                    Long matchId = match.getId();

                    matchQueueService.addPendingMatch(
                            opponentId,
                            userId,
                            matchId
                    );

                    // ✅ CLEANUP BOTH USERS
                    queue.remove(opponentId);
                    queue.remove(userId);
                    waitingStartTime.remove(opponentId);
                    waitingStartTime.remove(userId);

                    return Optional.of(matchId);
                }
            }

            // no opponent found
            queue.add(userId);
            waitingStartTime.put(userId, System.currentTimeMillis());

            return Optional.of(-1L);
        }
    }

    // CHECK MATCH
    public Optional<Long> checkMatch(Long userId, GameType gameType) {

        if (gameType == null) gameType = GameType.STANDARD;

        Set<Long> queue = waitingPlayers.get(gameType);

        Long matchId = matchQueueService.getPendingMatchId(userId);
        if (matchId != null) {
            queue.remove(userId);
            waitingStartTime.remove(userId);
            return Optional.of(matchId);
        }

        Long startTime = waitingStartTime.get(userId);
        if (startTime != null) {
            long waited = System.currentTimeMillis() - startTime;
            if (waited > MAX_WAIT_TIME) {
                queue.remove(userId);
                waitingStartTime.remove(userId);
                return Optional.of(-2L); // timeout
            }
        }

        return Optional.of(-1L); // still waiting
    }

    // CANCEL QUEUE
    public boolean cancelQueue(Long userId, GameType gameType) {

        if (gameType == null) gameType = GameType.STANDARD;

        Set<Long> queue = waitingPlayers.get(gameType);
        if (queue == null) return false;

        waitingStartTime.remove(userId);
        return queue.remove(userId);
    }
}

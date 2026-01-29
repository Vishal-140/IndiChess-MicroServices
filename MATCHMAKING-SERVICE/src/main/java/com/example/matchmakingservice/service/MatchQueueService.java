package com.example.matchmakingservice.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MatchQueueService {

    // userId -> matchId
    private final Map<Long, Long> userToMatchId = new ConcurrentHashMap<>();

    // matchId -> userIds
    private final Map<Long, Set<Long>> matchToUsers = new ConcurrentHashMap<>();

    // SAVE MATCH
    public void addPendingMatch(Long player1Id, Long player2Id, Long matchId) {

        userToMatchId.put(player1Id, matchId);
        userToMatchId.put(player2Id, matchId);

        Set<Long> users = ConcurrentHashMap.newKeySet();
        users.add(player1Id);
        users.add(player2Id);

        matchToUsers.put(matchId, users);
    }

    // CHECK MATCH
    public Long getPendingMatchId(Long userId) {

        Long matchId = userToMatchId.remove(userId);
        if (matchId == null) return null;

        // cleanup both players
        Set<Long> users = matchToUsers.remove(matchId);
        if (users != null) {
            for (Long id : users) {
                userToMatchId.remove(id);
            }
        }

        return matchId;
    }
}

package com.example.matchmakingservice.repo;

import com.example.matchmakingservice.entity.GameType;
import com.example.matchmakingservice.entity.MatchQueueEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MatchQueueRepo extends JpaRepository<MatchQueueEntry, Long> {

    // Find the oldest request for this game type that IS NOT the current user
    Optional<MatchQueueEntry> findFirstByGameTypeAndUserIdNotOrderByRequestTimeAsc(GameType gameType, Long userId);

    boolean existsByUserId(Long userId);
    
    void deleteByUserId(Long userId);

    Optional<MatchQueueEntry> findByUserId(Long userId);
}

package com.example.matchmakingservice.repo;

import com.example.matchmakingservice.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface MatchRepo extends JpaRepository<Match, Long> {
    Optional<Match> findFirstByPlayer1IdOrPlayer2IdOrderByCreatedAtDesc(Long p1, Long p2);
}

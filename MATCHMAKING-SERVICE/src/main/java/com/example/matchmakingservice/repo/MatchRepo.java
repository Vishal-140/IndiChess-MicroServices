package com.example.matchmakingservice.repo;

import com.example.matchmakingservice.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchRepo extends JpaRepository<Match, Long> {
}

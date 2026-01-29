package com.example.gameservice.repo;

import com.example.gameservice.entity.Move;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MoveRepo extends JpaRepository<Move, Long> {

    List<Move> findByGameIdOrderByPlyAsc(Long gameId);
}

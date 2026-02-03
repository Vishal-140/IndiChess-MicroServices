package com.example.matchmakingservice.entity;

import com.example.matchmakingservice.entity.GameType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "match_queue", indexes = {
    @Index(name = "idx_game_type_request_time", columnList = "gameType, requestTime")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class MatchQueueEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameType gameType;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime requestTime;
}

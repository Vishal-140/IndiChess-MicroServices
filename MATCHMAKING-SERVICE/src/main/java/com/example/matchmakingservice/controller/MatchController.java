package com.example.matchmakingservice.controller;

import com.example.matchmakingservice.dto.MatchResponse;
import com.example.matchmakingservice.entity.GameType;
import com.example.matchmakingservice.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/matchmaking")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    // JOIN QUEUE
    @PostMapping("/join")
    public MatchResponse joinQueue(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestParam(defaultValue = "STANDARD") GameType gameType
    ) {
        Long result = matchService.joinQueue(userId, gameType)
                .orElse(-2L);

        return new MatchResponse(result);
    }

    // CHECK MATCH
    @GetMapping("/check")
    public MatchResponse checkMatch(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestParam(defaultValue = "STANDARD") GameType gameType
    ) {
        Long result = matchService.checkMatch(userId, gameType)
                .orElse(-2L);

        return new MatchResponse(result);
    }

    // CANCEL QUEUE
    @PostMapping("/cancel")
    public MatchResponse cancelQueue(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestParam(defaultValue = "STANDARD") GameType gameType
    ) {
        boolean cancelled = matchService.cancelQueue(userId, gameType);
        return new MatchResponse(cancelled ? 1L : 0L);
    }
}
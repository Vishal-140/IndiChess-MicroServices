package com.example.matchmakingservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.example.matchmakingservice.entity.GameType;

@FeignClient(name = "GAME-SERVICE")
public interface GameServiceClient {

    @PostMapping("/game/create")
    Long createGame(
            @RequestParam("whitePlayerId") Long whitePlayerId,
            @RequestParam("blackPlayerId") Long blackPlayerId,
            @RequestParam("matchId") Long matchId,
            @RequestParam("gameType") GameType gameType
    );
}

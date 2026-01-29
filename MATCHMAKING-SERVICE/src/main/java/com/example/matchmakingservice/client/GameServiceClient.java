package com.example.matchmakingservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "GAME-SERVICE")
public interface GameServiceClient {

    @PostMapping("/game/create")
    Long createGame(
            @RequestParam("whitePlayerId") Long whitePlayerId,
            @RequestParam("blackPlayerId") Long blackPlayerId,
            @RequestParam("matchId") Long matchId
    );
}

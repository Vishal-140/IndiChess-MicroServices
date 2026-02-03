package com.example.gameservice.controller;

import com.example.gameservice.dto.MoveRequest;
import com.example.gameservice.dto.MoveResponse;
import com.example.gameservice.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class GameSocketController {

    private final GameService gameService;

    @MessageMapping("/game/{gameId}/move")
    public void processMove(
            @DestinationVariable Long gameId,
            @Payload MoveRequest moveRequest,
            @Header(value = "X-USER-ID", required = false) String userIdStr
    ) {
        
        Long userId = (userIdStr != null) ? Long.valueOf(userIdStr) : 0L; // Fallback or throw
        
        // Call service (which also broadcasts the result)
        gameService.makeMove(gameId, userId, moveRequest);
    }
}

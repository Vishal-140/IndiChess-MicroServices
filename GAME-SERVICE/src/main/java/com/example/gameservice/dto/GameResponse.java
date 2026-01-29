package com.example.gameservice.dto;

import com.example.gameservice.entity.Game;
import lombok.Data;

@Data
public class GameResponse {

    private Long gameId;
    private Long player1Id;
    private Long player2Id;
    private String status;
    private String gameType;

    private Integer currentPly;
    private String fen;
    private Integer whiteTime;
    private Integer blackTime;

    public static GameResponse from(Game game) {
        GameResponse res = new GameResponse();
        res.setGameId(game.getId());
        res.setPlayer1Id(game.getPlayer1Id());
        res.setPlayer2Id(game.getPlayer2Id());
        res.setStatus(game.getStatus().name());
        res.setGameType(game.getGameType().name());

        res.setCurrentPly(game.getCurrentPly());
        res.setFen(game.getFenCurrent());
        res.setWhiteTime(game.getWhiteTime());
        res.setBlackTime(game.getBlackTime());

        return res;
    }
}

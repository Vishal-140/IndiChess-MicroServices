package com.example.gameservice.dto;

import lombok.Data;

@Data
public class MoveResponse {

    private Long gameId;

    private String uci;         // e2e4
    private String fen;         // updated FEN

    private Integer currentPly;

    private Integer whiteTime;
    private Integer blackTime;

    private String nextTurn;    // WHITE / BLACK
    private String status;      // ONGOING / FINISHED


}

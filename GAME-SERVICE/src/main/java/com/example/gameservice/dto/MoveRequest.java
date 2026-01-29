package com.example.gameservice.dto;

import lombok.Data;

@Data
public class MoveRequest {

    // chess move
    private String uci;          // e2e4, g1f3
    private String promotion;    // q, r, b, n (optional)
    private Boolean resign;
    private Boolean offerDraw;

}

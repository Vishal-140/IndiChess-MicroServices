package com.example.gameservice.entity;

import java.util.Arrays;

public enum GameStatus {

    IN_PROGRESS(-2),
    PLAYER1_WON(-1),
    DRAW(0),
    PLAYER2_WON(1);

    private final int code;

    GameStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static GameStatus fromCode(int code) {
        return Arrays.stream(values())
                .filter(status -> status.code == code)
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid GameStatus code: " + code));
    }
}

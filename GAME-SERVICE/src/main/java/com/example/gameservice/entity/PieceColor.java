package com.example.gameservice.entity;

public enum PieceColor {

    WHITE,
    BLACK;

    // ply: 1 -> WHITE, 2 -> BLACK
    public static PieceColor fromPly(int ply) {
        return (ply % 2 == 1) ? WHITE : BLACK;
    }

    public PieceColor opposite() {
        return this == WHITE ? BLACK : WHITE;
    }
}

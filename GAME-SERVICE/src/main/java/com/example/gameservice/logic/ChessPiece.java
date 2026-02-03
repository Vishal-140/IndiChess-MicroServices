package com.example.gameservice.logic;

import com.example.gameservice.entity.PieceColor;

public enum ChessPiece {
    WHITE_PAWN(PieceColor.WHITE, Type.PAWN, 'P'),
    WHITE_ROOK(PieceColor.WHITE, Type.ROOK, 'R'),
    WHITE_KNIGHT(PieceColor.WHITE, Type.KNIGHT, 'N'),
    WHITE_BISHOP(PieceColor.WHITE, Type.BISHOP, 'B'),
    WHITE_QUEEN(PieceColor.WHITE, Type.QUEEN, 'Q'),
    WHITE_KING(PieceColor.WHITE, Type.KING, 'K'),

    BLACK_PAWN(PieceColor.BLACK, Type.PAWN, 'p'),
    BLACK_ROOK(PieceColor.BLACK, Type.ROOK, 'r'),
    BLACK_KNIGHT(PieceColor.BLACK, Type.KNIGHT, 'n'),
    BLACK_BISHOP(PieceColor.BLACK, Type.BISHOP, 'b'),
    BLACK_QUEEN(PieceColor.BLACK, Type.QUEEN, 'q'),
    BLACK_KING(PieceColor.BLACK, Type.KING, 'k');

    public enum Type {
        PAWN, ROOK, KNIGHT, BISHOP, QUEEN, KING
    }

    private final PieceColor color;
    private final Type type;
    private final char fenChar;

    ChessPiece(PieceColor color, Type type, char fenChar) {
        this.color = color;
        this.type = type;
        this.fenChar = fenChar;
    }

    public PieceColor getColor() {
        return color;
    }

    public Type getType() {
        return type;
    }

    public char getFenChar() {
        return fenChar;
    }

    public static ChessPiece fromFenChar(char c) {
        for (ChessPiece p : values()) {
            if (p.fenChar == c) return p;
        }
        return null;
    }
}

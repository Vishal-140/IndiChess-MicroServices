package com.example.gameservice.logic;

import com.example.gameservice.entity.PieceColor;

public class ChessBoard {

    private final ChessPiece[][] board = new ChessPiece[8][8];
    private PieceColor activeColor;
    private String castleRights;
    private String enPassantTarget;
    private int halfMoveClock;
    private int fullMoveNumber;

    public ChessBoard(String fen) {
        loadFen(fen);
    }

    public ChessPiece getPiece(int rank, int file) {
        if (!isWithinBounds(rank, file)) return null;
        return board[rank][file];
    }

    public void setPiece(int rank, int file, ChessPiece piece) {
        if (isWithinBounds(rank, file)) {
            board[rank][file] = piece;
        }
    }

    public boolean isWithinBounds(int rank, int file) {
        return rank >= 0 && rank < 8 && file >= 0 && file < 8;
    }

    public PieceColor getActiveColor() {
        return activeColor;
    }

    public String getCastleRights() {
        return castleRights;
    }

    public void setEnPassantTarget(String enPassantTarget) {
        this.enPassantTarget = enPassantTarget;
    }

    public String getEnPassantTarget() {
        return enPassantTarget;
    }

    public int getHalfMoveClock() {
        return halfMoveClock;
    }

    public int getFullMoveNumber() {
        return fullMoveNumber;
    }

    // --- STATE MUTATION METHODS ---

    public void incrementHalfMoveClock() {
        this.halfMoveClock++;
    }

    public void resetHalfMoveClock() {
        this.halfMoveClock = 0;
    }

    public void incrementFullMoveNumber() {
        this.fullMoveNumber++;
    }

    public void switchActiveColor() {
        this.activeColor = (this.activeColor == PieceColor.WHITE) ? PieceColor.BLACK : PieceColor.WHITE;
    }

    /**
     * Revoke castling rights for a specific side/color.
     * @param white true for White, false for Black
     * @param kingSide true to revoke Kingside (K/k)
     * @param queenSide true to revoke Queenside (Q/q)
     */
    public void revokeCastlingRights(boolean white, boolean kingSide, boolean queenSide) {
        if (this.castleRights.equals("-")) return;

        StringBuilder sb = new StringBuilder();
        // We rebuild the string excluding what we want to remove
        for (char c : this.castleRights.toCharArray()) {
            boolean keep = true;
            if (white) {
                if (kingSide && c == 'K') keep = false;
                if (queenSide && c == 'Q') keep = false;
            } else {
                if (kingSide && c == 'k') keep = false;
                if (queenSide && c == 'q') keep = false;
            }
            if (keep) sb.append(c);
        }

        String newRights = sb.toString();
        this.castleRights = newRights.isEmpty() ? "-" : newRights;
    }

    /**
     * Revoke ALL castling rights for a color (e.g., King moved).
     */
    public void revokeAllCastlingRights(boolean white) {
        revokeCastlingRights(white, true, true);
    }
    
    // --- FEN PARSING ---

    private void loadFen(String fen) {
        String[] parts = fen.split(" ");
        String placement = parts[0];
        String turn = parts[1];
        this.castleRights = parts[2];
        this.enPassantTarget = parts[3];
        this.halfMoveClock = Integer.parseInt(parts[4]);
        this.fullMoveNumber = Integer.parseInt(parts[5]);

        this.activeColor = turn.equals("w") ? PieceColor.WHITE : PieceColor.BLACK;

        // Parse placement
        String[] rows = placement.split("/");
        for (int r = 0; r < 8; r++) {
            String row = rows[r];
            int file = 0;
            for (char c : row.toCharArray()) {
                if (Character.isDigit(c)) {
                    file += Character.getNumericValue(c);
                } else {
                    board[7 - r][file] = ChessPiece.fromFenChar(c); // Rank 0 is bottom (row index 7 in FEN is rank 0?)
                    // Wait, standard convention: 
                    // array[0][0] = a1. 
                    // FEN starts at rank 8 (index 7 in 0-7 scale).
                    // So FEN row 0 is Rank 8.
                    // Let's stick to: Rank 0 = A1..H1. Rank 7 = A8..H8.
                    // FEN row 0 maps to Rank 7.
                    // FEN row 1 maps to Rank 6.
                    
                    // Actually, let's fix parsing logic:
                    // loop r from 0 to 7.
                    // Rank index = 7 - r.
                    
                    file++;
                }
            }
        }
    }

    // --- HELPER to Switch Turn ---
    
    public String toFenWithNextTurn() {
        switchActiveColor();
        if (this.activeColor == PieceColor.WHITE) {
            incrementFullMoveNumber();
        }
        return toFen();
    }
    
    public String toFen() {
        StringBuilder sb = new StringBuilder();

        // 1. Placement
        for (int r = 7; r >= 0; r--) {
            int emptyCount = 0;
            for (int f = 0; f < 8; f++) {
                ChessPiece p = board[r][f];
                if (p == null) {
                    emptyCount++;
                } else {
                    if (emptyCount > 0) {
                        sb.append(emptyCount);
                        emptyCount = 0;
                    }
                    sb.append(p.getFenChar());
                }
            }
            if (emptyCount > 0) {
                sb.append(emptyCount);
            }
            if (r > 0) {
                sb.append("/");
            }
        }

        // 2. Active Color
        sb.append(" ").append(activeColor == PieceColor.WHITE ? "w" : "b");

        // 3. Castle
        sb.append(" ").append(castleRights);

        // 4. En Passant
        sb.append(" ").append(enPassantTarget);

        // 5. Clocks
        sb.append(" ").append(halfMoveClock).append(" ").append(fullMoveNumber);

        return sb.toString();
    }
    
    public int[] findKing(PieceColor color) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                ChessPiece p = board[r][c];
                if (p != null && p.getType() == ChessPiece.Type.KING && p.getColor() == color) {
                    return new int[]{r, c};
                }
            }
        }
        return null;
    }
    
    // Helper: Copy constructor for simulation
    public ChessBoard(ChessBoard other) {
        this.activeColor = other.activeColor;
        this.castleRights = other.castleRights;
        this.enPassantTarget = other.enPassantTarget;
        this.halfMoveClock = other.halfMoveClock;
        this.fullMoveNumber = other.fullMoveNumber;
        
        for(int r=0; r<8; r++) {
            System.arraycopy(other.board[r], 0, this.board[r], 0, 8);
        }
    }
}

package com.example.gameservice.logic;

import com.example.gameservice.entity.PieceColor;

public class MoveValidator {

    public static boolean isLegalMove(ChessBoard board, String uci, boolean checkTurn) {
        if (uci == null || uci.length() < 4) return false;

        // Parse UCI (e.g., "e2e4")
        int fromFile = uci.charAt(0) - 'a';
        int fromRank = uci.charAt(1) - '1';
        int toFile = uci.charAt(2) - 'a';
        int toRank = uci.charAt(3) - '1';

        // 1. Basic Bounds Check
        if (!board.isWithinBounds(fromRank, fromFile) || !board.isWithinBounds(toRank, toFile)) {
            return false;
        }

        ChessPiece piece = board.getPiece(fromRank, fromFile);
        if (piece == null) return false;

        // 2. Turn Check
        if (checkTurn && piece.getColor() != board.getActiveColor()) {
            return false;
        }

        // 3. Target Square Check (Constraint: Cannot capture own piece)
        ChessPiece target = board.getPiece(toRank, toFile);
        if (target != null && target.getColor() == piece.getColor()) {
            return false;
        }

        // 4. Piece-Specific Move Rules (Pseudo-legal)
        if (!isPieceMoveValid(board, piece, fromRank, fromFile, toRank, toFile)) {
            return false;
        }

        // 5. King Safety Check (Does this move leave King in check?)
        ChessBoard simulatedBoard = new ChessBoard(board);
        // Apply move on simulated board
        simulatedBoard.setPiece(toRank, toFile, piece);
        simulatedBoard.setPiece(fromRank, fromFile, null);
        
        // Handle Castling King Move simulation
         if (piece.getType() == ChessPiece.Type.KING && Math.abs(toFile - fromFile) == 2) {
             boolean isKingside = toFile > fromFile;
             int rookTx = isKingside ? 7 : 0;
             int rookFx = isKingside ? 5 : 3;
             ChessPiece rook = simulatedBoard.getPiece(toRank, rookTx);
             simulatedBoard.setPiece(toRank, rookFx, rook);
             simulatedBoard.setPiece(toRank, rookTx, null);
         }

        if (isKingInCheck(simulatedBoard, piece.getColor())) {
            return false;
        }
        
        return true; 
    }

    public static boolean isKingInCheck(ChessBoard board, PieceColor kingColor) {
        int[] kingPos = board.findKing(kingColor);
        if (kingPos == null) return false; // Should not happen
        return isSquareAttacked(board, kingPos[0], kingPos[1], kingColor == PieceColor.WHITE ? PieceColor.BLACK : PieceColor.WHITE);
    }

    public static boolean isSquareAttacked(ChessBoard board, int r, int c, PieceColor attackerColor) {
        // Iterate all attacker pieces and see if they can move to (r, c)
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                ChessPiece p = board.getPiece(i, j);
                if (p != null && p.getColor() == attackerColor) {
                    // Check if this piece can capture (r, c)
                    // Note: pawn capture is different from movement
                    if (p.getType() == ChessPiece.Type.PAWN) {
                        int direction = (p.getColor() == PieceColor.WHITE) ? 1 : -1;
                        int dr = r - i;
                        int dc = Math.abs(c - j);
                        if (dr == direction && dc == 1) return true;
                    } else {
                        // For others, reuse validation logic (ignoring turn/self-capture checks)
                        if (isPieceMoveValid(board, p, i, j, r, c)) return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean hasAnyLegalMove(ChessBoard board, PieceColor color) {
        // Iterate all pieces of 'color'
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                ChessPiece p = board.getPiece(r, c);
                if (p != null && p.getColor() == color) {
                    // Try all possible moves
                    for (int tr = 0; tr < 8; tr++) {
                        for (int tc = 0; tc < 8; tc++) {
                            // Optimisation: Skip if target is same color
                             ChessPiece target = board.getPiece(tr, tc);
                             if (target != null && target.getColor() == color) continue;

                            if (isLegalMove(board, "" + (char)('a' + c) + (char)('1' + r) + (char)('a' + tc) + (char)('1' + tr), false)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private static boolean isPieceMoveValid(ChessBoard board, ChessPiece piece, int r1, int c1, int r2, int c2) {
        int dr = r2 - r1;
        int dc = c2 - c1;
        int absDr = Math.abs(dr);
        int absDc = Math.abs(dc);

        switch (piece.getType()) {
            case PAWN:
                return isPawnMoveValid(board, piece, r1, c1, r2, c2, dr, dc, absDr, absDc);
            case ROOK:
                return (r1 == r2 || c1 == c2) && isPathClear(board, r1, c1, r2, c2);
            case KNIGHT:
                return (absDr == 2 && absDc == 1) || (absDr == 1 && absDc == 2);
            case BISHOP:
                return (absDr == absDc) && isPathClear(board, r1, c1, r2, c2);
            case QUEEN:
                return (r1 == r2 || c1 == c2 || absDr == absDc) && isPathClear(board, r1, c1, r2, c2);
            case KING:
                if (absDr <= 1 && absDc <= 1) return true;
                // Castling
                if (r1 == r2 && absDc == 2) {
                    return isLegalCastling(board, piece, r1, c1, r2, c2);
                }
                return false;
            default:
                return false;
        }
    }

    private static boolean isPawnMoveValid(ChessBoard board, ChessPiece piece, int r1, int c1, int r2, int c2, int dr, int dc, int absDr, int absDc) {
        int direction = (piece.getColor() == PieceColor.WHITE) ? 1 : -1;
        int startRank = (piece.getColor() == PieceColor.WHITE) ? 1 : 6;
        ChessPiece target = board.getPiece(r2, c2);

        // Forward Move (non-capture)
        if (dc == 0) {
            // Single step
            if (dr == direction) {
                return target == null;
            }
            // Double step from start
            if (dr == 2 * direction && r1 == startRank) {
                return target == null && board.getPiece(r1 + direction, c1) == null;
            }
        }
        // Capture (diagonal)
        else if (absDc == 1 && dr == direction) {
            // Normal capture
            if (target != null && target.getColor() != piece.getColor()) {
                return true;
            }
            // En Passant
            String targetSq = "" + (char)('a' + c2) + (char)('1' + r2);
            if (target == null && targetSq.equals(board.getEnPassantTarget())) {
                return true;
            }
        }

        return false;
    }

    private static boolean isLegalCastling(ChessBoard board, ChessPiece piece, int r1, int c1, int r2, int c2) {
        String rights = board.getCastleRights();
        boolean isWhite = piece.getColor() == PieceColor.WHITE;

        // 0. General Rule: Cannot castle if currently in check
        if (isKingInCheck(board, piece.getColor())) {
            return false;
        }

        // Kingside (e.g., e1 -> g1)
        if (c2 > c1) {
             char requiredRight = isWhite ? 'K' : 'k';
             if (rights.indexOf(requiredRight) == -1) return false;
             // Check path clear (f1, g1)
             if (board.getPiece(r1, 5) != null || board.getPiece(r1, 6) != null) return false;
             
             // Check path safe (f1 cannot be attacked)
             // Attacker color is opposite
             PieceColor riskColor = isWhite ? PieceColor.BLACK : PieceColor.WHITE;
             if (isSquareAttacked(board, r1, 5, riskColor)) return false;
             
             // Destination (g1) safety is checked by general "isKingInCheck" after simulation,
             // but strictly checking it here is fine/redundant.
             if (isSquareAttacked(board, r1, 6, riskColor)) return false;
        } 
        // Queenside (e.g., e1 -> c1)
        else {
             char requiredRight = isWhite ? 'Q' : 'q';
             if (rights.indexOf(requiredRight) == -1) return false;
             // Check path clear (d1, c1, b1)
             if (board.getPiece(r1, 3) != null || board.getPiece(r1, 2) != null || board.getPiece(r1, 1) != null) return false;
             
             // Check path safe (d1 cannot be attacked)
             PieceColor riskColor = isWhite ? PieceColor.BLACK : PieceColor.WHITE;
             if (isSquareAttacked(board, r1, 3, riskColor)) return false;
             
             // Destination (c1) checked by isKingInCheck after simulation
             if (isSquareAttacked(board, r1, 2, riskColor)) return false;
        }
        return true;
    }

    private static boolean isPathClear(ChessBoard board, int r1, int c1, int r2, int c2) {
        int dx = Integer.compare(c2, c1);
        int dy = Integer.compare(r2, r1);

        int currR = r1 + dy;
        int currC = c1 + dx;

        while (currR != r2 || currC != c2) {
            if (board.getPiece(currR, currC) != null) {
                return false;
            }
            currR += dy;
            currC += dx;
        }
        return true;
    }
}

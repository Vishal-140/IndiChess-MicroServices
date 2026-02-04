package com.example.gameservice.logic;

import com.example.gameservice.entity.PieceColor;

public class GameEngine {

    public static String applyMove(String currentFen, String uciMove) {
        ChessBoard board = new ChessBoard(currentFen);
        
        // 1. Validate
        if (!MoveValidator.isLegalMove(board, uciMove, true)) {
            throw new IllegalArgumentException("Illegal move: " + uciMove);
        }

        // 2. Apply Move
        // Parse UCI
        int fromFile = uciMove.charAt(0) - 'a';
        int fromRank = uciMove.charAt(1) - '1';
        int toFile = uciMove.charAt(2) - 'a';
        int toRank = uciMove.charAt(3) - '1';

        // Detect Capture (before moving)
        boolean isCapture = board.getPiece(toRank, toFile) != null;
        
        // Handle Castling Rights (Rook Capture)
        if (isCapture) {
            ChessPiece captured = board.getPiece(toRank, toFile);
            if (captured.getType() == ChessPiece.Type.ROOK) {
                if (toRank == 0 && toFile == 0) board.revokeCastlingRights(true, false, true); // White Queenside
                if (toRank == 0 && toFile == 7) board.revokeCastlingRights(true, true, false); // White Kingside
                if (toRank == 7 && toFile == 0) board.revokeCastlingRights(false, false, true); // Black Queenside
                if (toRank == 7 && toFile == 7) board.revokeCastlingRights(false, true, false); // Black Kingside
            }
        }

        ChessPiece piece = board.getPiece(fromRank, fromFile);
        
        // Handle Castling Rights (Moving King or Rook)
        if (piece.getType() == ChessPiece.Type.KING) {
            board.revokeAllCastlingRights(piece.getColor() == PieceColor.WHITE);
        } else if (piece.getType() == ChessPiece.Type.ROOK) {
             if (fromRank == 0 && fromFile == 0) board.revokeCastlingRights(true, false, true);
             if (fromRank == 0 && fromFile == 7) board.revokeCastlingRights(true, true, false);
             if (fromRank == 7 && fromFile == 0) board.revokeCastlingRights(false, false, true);
             if (fromRank == 7 && fromFile == 7) board.revokeCastlingRights(false, true, false);
        }

        // Apply Move to Board
        board.setPiece(toRank, toFile, piece);
        board.setPiece(fromRank, fromFile, null);
        
        // Update HalfMove Clock
        if (piece.getType() == ChessPiece.Type.PAWN || isCapture) {
            board.resetHalfMoveClock();
        } else {
            board.incrementHalfMoveClock();
        }

        // Reset En Passant Target by default
        board.setEnPassantTarget("-");

        // Handle Pawn Promotion and Special Moves
        if (piece.getType() == ChessPiece.Type.PAWN) {
             int dr = toRank - fromRank;

             // 1. Double Push -> Set En Passant Target
             if (Math.abs(dr) == 2) {
                 int pRank = (fromRank + toRank) / 2;
                 String t = "" + (char)('a' + fromFile) + (char)('1' + pRank);
                 board.setEnPassantTarget(t);
             }

             // 2. En Passant Capture Execution
             if (Math.abs(toFile - fromFile) == 1 && !isCapture) {
                 // It was an en passant capture (diagonal move to empty square)
                 isCapture = true; // Update flag
                 int capturedRank = (piece.getColor() == PieceColor.WHITE) ? toRank - 1 : toRank + 1;
                 board.setPiece(capturedRank, toFile, null);
                 board.resetHalfMoveClock(); // Ensure reset
             }

             // 3. Promotion
             if ((piece.getColor() == PieceColor.WHITE && toRank == 7) ||
                 (piece.getColor() == PieceColor.BLACK && toRank == 0)) {
                 
                 ChessPiece promotedPiece;
                 // Parse promotion char from UCI (e.g., e7e8q)
                 if (uciMove.length() == 5) {
                     char promoChar = uciMove.charAt(4);
                     promotedPiece = switch (Character.toLowerCase(promoChar)) {
                         case 'r' -> (piece.getColor() == PieceColor.WHITE) ? ChessPiece.WHITE_ROOK : ChessPiece.BLACK_ROOK;
                         case 'b' -> (piece.getColor() == PieceColor.WHITE) ? ChessPiece.WHITE_BISHOP : ChessPiece.BLACK_BISHOP;
                         case 'n' -> (piece.getColor() == PieceColor.WHITE) ? ChessPiece.WHITE_KNIGHT : ChessPiece.BLACK_KNIGHT;
                         default -> (piece.getColor() == PieceColor.WHITE) ? ChessPiece.WHITE_QUEEN : ChessPiece.BLACK_QUEEN; 
                     };
                 } else {
                     // Default to Queen
                     promotedPiece = (piece.getColor() == PieceColor.WHITE) ? ChessPiece.WHITE_QUEEN : ChessPiece.BLACK_QUEEN;
                 }
                 board.setPiece(toRank, toFile, promotedPiece);
             }
        }
        
        // Handle Castling Execution (Move Rook)
        if (piece.getType() == ChessPiece.Type.KING && Math.abs(toFile - fromFile) == 2) {
            boolean isKingside = toFile > fromFile;
            int rookTx = isKingside ? 7 : 0;
            int rookFx = isKingside ? 5 : 3;
            
            ChessPiece rook = board.getPiece(toRank, rookTx);
            board.setPiece(toRank, rookFx, rook);
            board.setPiece(toRank, rookTx, null);
        }

        // 3. Update State (Turn)
        return board.toFenWithNextTurn(); 
    }
    
    public static com.example.gameservice.entity.GameStatus getGameStatus(String fen, java.util.List<String> historyFens) {
        ChessBoard board = new ChessBoard(fen);
        PieceColor activeColor = board.getActiveColor();
        
        // 1. Checkmate / Stalemate
        boolean inCheck = MoveValidator.isKingInCheck(board, activeColor);
        boolean hasMoves = MoveValidator.hasAnyLegalMove(board, activeColor);
        
        if (inCheck && !hasMoves) {
             return activeColor == PieceColor.WHITE ? 
                 com.example.gameservice.entity.GameStatus.BLACK_WON : // White is mated
                 com.example.gameservice.entity.GameStatus.WHITE_WON;
        }
        
        if (!inCheck && !hasMoves) {
            return com.example.gameservice.entity.GameStatus.DRAW; // Stalemate
        }
        
        // 2. Insufficient Material
        if (isInsufficientMaterial(board)) {
            return com.example.gameservice.entity.GameStatus.DRAW;
        }
        
        // 3. 50-Move Rule
        if (board.getHalfMoveClock() >= 100) {
            return com.example.gameservice.entity.GameStatus.DRAW;
        }
        
        // 4. 3-Fold Repetition
        if (checkRepetition(fen, historyFens)) {
            return com.example.gameservice.entity.GameStatus.DRAW;
        }
        
        return com.example.gameservice.entity.GameStatus.IN_PROGRESS;
    }
    
    // Legacy support if needed, but we should migrate calls
    public static com.example.gameservice.entity.GameStatus getGameStatus(String fen) {
        return getGameStatus(fen, java.util.Collections.emptyList());
    }

    private static boolean isInsufficientMaterial(ChessBoard board) {
        int whitePieces = 0;
        int blackPieces = 0;
        int whiteBishops = 0; // or Knights
        int blackBishops = 0; // or Knights
        
        for (int r = 0; r < 8; r++) {
            for (int f = 0; f < 8; f++) {
                ChessPiece p = board.getPiece(r, f);
                if (p == null) continue;
                
                if (p.getType() == ChessPiece.Type.PAWN || 
                    p.getType() == ChessPiece.Type.ROOK || 
                    p.getType() == ChessPiece.Type.QUEEN) {
                    return false; // Pawns, Rooks, Queens are sufficient
                }
                
                if (p.getColor() == PieceColor.WHITE) {
                    if (p.getType() != ChessPiece.Type.KING) {
                        whitePieces++;
                        whiteBishops++; // Count minor pieces
                    }
                } else {
                    if (p.getType() != ChessPiece.Type.KING) {
                        blackPieces++;
                        blackBishops++; // Count minor pieces
                    }
                }
            }
        }
        
        // K vs K
        if (whitePieces == 0 && blackPieces == 0) return true;
        
        // K + Minor vs K
        if ((whitePieces == 1 && blackPieces == 0) || (whitePieces == 0 && blackPieces == 1)) return true;
        
        // K + B vs K + B (same color bishops) - simplified: just treat K+Minor vs K+Minor as insufficient?
        // Standard FIDE: K+B vs K+B is draw IF bishops are same color.
        // For MVP simplicity: K+Minor vs K+Minor is OFTEN valid draw, but technically checkmate possible if opposing bishops?
        // Let's stick to safe insufficient: K vs K, K+N vs K, K+B vs K.
        // K+N vs K+N is NOT automatically insufficient (unlikely but matte possible).
        
        return false;
    }
    
    private static boolean checkRepetition(String currentFen, java.util.List<String> historyFens) {
        if (historyFens == null || historyFens.isEmpty()) return false;
        
        String currentKey = getFenRepetitionKey(currentFen);
        int count = 1; // Current position counts as 1
        
        for (String f : historyFens) {
            if (getFenRepetitionKey(f).equals(currentKey)) {
                count++;
            }
        }
        
        return count >= 3;
    }
    
    private static String getFenRepetitionKey(String fen) {
        // Only Placement + Turn + Castling + EnPassant matters. Clocks do not.
        String[] parts = fen.split(" ");
        if (parts.length < 4) return fen;
        return parts[0] + " " + parts[1] + " " + parts[2] + " " + parts[3];
    }
}

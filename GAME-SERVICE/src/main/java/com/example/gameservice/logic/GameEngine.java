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

        ChessPiece piece = board.getPiece(fromRank, fromFile);
        board.setPiece(toRank, toFile, piece);
        board.setPiece(fromRank, fromFile, null);
        
        // Reset En Passant Target by default
        board.setEnPassantTarget("-");

        // TODO: Handle Pawn Promotion (e.g. "e7e8q")
        if (piece.getType() == ChessPiece.Type.PAWN) {
             int dr = toRank - fromRank;

             // 1. Double Push -> Set En Passant Target
             if (Math.abs(dr) == 2) {
                 int pRank = (fromRank + toRank) / 2;
                 String t = "" + (char)('a' + fromFile) + (char)('1' + pRank);
                 board.setEnPassantTarget(t);
             }

             // 2. En Passant Capture Execution
             // If pawn moved diagonally to an empty square, it's En Passant
             if (Math.abs(toFile - fromFile) == 1 && board.getPiece(toRank, toFile) == null) {
                 // The captured pawn is "behind" the target square
                 // If WHITE moved up (rank +1), captured pawn is at toRank - 1
                 // If BLACK moved down (rank -1), captured pawn is at toRank + 1
                 int capturedRank = (piece.getColor() == PieceColor.WHITE) ? toRank - 1 : toRank + 1;
                 board.setPiece(capturedRank, toFile, null);
             }

             // 3. Promotion
             if ((piece.getColor() == PieceColor.WHITE && toRank == 7) ||
                 (piece.getColor() == PieceColor.BLACK && toRank == 0)) {
                 board.setPiece(toRank, toFile, 
                     piece.getColor() == PieceColor.WHITE ? ChessPiece.WHITE_QUEEN : ChessPiece.BLACK_QUEEN);
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

        // 3. Update State (Turn, Castling Rights, etc.)
        // This is a simplified update. A real engine needs detailed state tracking.
        // For now, we manually reconstruct the FEN by flipping the active color.
        
        // Toggle turn
        // Note: ChessBoard.toFen() uses internal activeColor field. 
        // We need a method to switch turn in ChessBoard or manually manipulate it.
        // For now, let's just use a string manipulation or add a method to ChessBoard.
        
        // Let's rely on constructing a NEW FEN or modifying the board state deeply.
        // Since ChessBoard is our state holder, we should add a `switchTurn()` method.
        
        return board.toFenWithNextTurn(); 
    }
    
    public static com.example.gameservice.entity.GameStatus getGameStatus(String fen) {
        ChessBoard board = new ChessBoard(fen);
        PieceColor activeColor = board.getActiveColor();
        
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
        
        return com.example.gameservice.entity.GameStatus.IN_PROGRESS;
    }
}

package com.example.gameservice.logic;

import com.example.gameservice.entity.PieceColor;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ChessLogicTest {

    @Test
    void testInitialBoard() {
        ChessBoard board = new ChessBoard("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        assertNotNull(board.getPiece(0, 0)); // a1 = Rook (White is rank 0 in logical array if mapped correctly, wait. Standard is Rank 0 = 1, Rank 7 = 8)
        // Let's verify mapping: 
        // Logic: Rank 0 = row 0. FEN "RNBQKBNR" is Rank 0 (White pieces)? 
        // No, FEN starts from Rank 8. 
        // My Code: 
        // rows = placement.split("/");
        // for (int r = 0; r < 8; r++) { ... using rows[r] }
        // board[7-r][file] = ...
        // So FEN row 0 (Rank 8) -> board[7] (Rank 8). Correct.
        // FEN row 7 (Rank 1) -> board[0] (Rank 1). Correct.
        
        // Check A1 (Rank 0, File 0) -> Should be White Rook
        assertEquals(ChessPiece.WHITE_ROOK, board.getPiece(0, 0));
        // Check E1 (Rank 0, File 4) -> White King
        assertEquals(ChessPiece.WHITE_KING, board.getPiece(0, 4));
        // Check E8 (Rank 7, File 4) -> Black King
        assertEquals(ChessPiece.BLACK_KING, board.getPiece(7, 4));
    }

    @Test
    void testPawnMove() {
        ChessBoard board = new ChessBoard("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        // e2e4 (Rank 1, File 4 -> Rank 3, File 4)
        // Check validity
        assertTrue(MoveValidator.isLegalMove(board, "e2e4", true));
        
        // Execute
        String newFen = GameEngine.applyMove("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", "e2e4");
        // Expected FEN: rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1
        assertTrue(newFen.contains("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR"));
        assertTrue(newFen.contains(" b ")); // Black to move
        assertTrue(newFen.contains(" e3 ")); // En Passant target
    }
    
    @Test
    void testCheckmateDetection() {
        // Fools Mate
        // 1. f3 e5 2. g4 Qh4#
        String fen = "rnbqkbnr/pppp1ppp/8/4p3/6P1/5P2/PPPPP2P/RNBQKBNR b KQkq - 0 2"; // Position after 2. g4
        // Check validity of Qh4
        assertTrue(MoveValidator.isLegalMove(new ChessBoard(fen), "d8h4", true));
        
        // Apply Move
        String mateFen = GameEngine.applyMove(fen, "d8h4");
        
        // Verify Game Status
        assertEquals(com.example.gameservice.entity.GameStatus.BLACK_WON, GameEngine.getGameStatus(mateFen));
    }
    
    @Test
    void testCheckPrevention() {
        // King at e1, Queen at e2. Black Rook at e8.
        // If Queen moves d1, King is exposed.
        // Fen: 4r3/8/8/8/8/8/4Q3/4K3 w - - 0 1
        String fen = "4r3/8/8/8/8/8/4Q3/4K3 w - - 0 1";
        ChessBoard board = new ChessBoard(fen);
        
        // Move Queen d1 -> Exposed Check
        assertFalse(MoveValidator.isLegalMove(board, "e2d1", true));
        
        // Move King f1 -> Safe
        assertTrue(MoveValidator.isLegalMove(board, "e1f1", true));
    }
}

import React from "react";
import Player from "./Player";
import Board from "./Board";
import "../component-styles/BoardLayout.css";
import Clock from "./Clock";

const BoardLayout = ({
  addMove, fen, userColor, statusMessage,
  whiteTime, blackTime, lastMoveTimestamp, isWhiteTurn,
  onResign, onOfferDraw, onRespondDraw, drawOfferedBy, userId
}) => {

  const isWhite = userColor === 'w';
  const myTurn = (userColor === 'w' && isWhiteTurn) || (userColor === 'b' && !isWhiteTurn);

  // Identify active states for styling
  const topPlayerActive = isWhite ? !isWhiteTurn : isWhiteTurn;
  const bottomPlayerActive = isWhite ? isWhiteTurn : !isWhiteTurn;

  // Modal logic
  const showDrawModal = drawOfferedBy && String(drawOfferedBy) !== String(userId) && drawOfferedBy !== "REJECTED";
  const isGameOver = statusMessage && statusMessage.startsWith("Game Over");

  return (
    <div className="board-layout-container">

      {/* Game Over Modal */}
      {isGameOver && (
        <div className="game-modal-overlay">
          <div className="game-modal">
            <h2>Game Over</h2>
            <h3>{statusMessage.replace("Game Over: ", "")}</h3>
            <button onClick={() => window.location.href = "/home"}>Back to Home</button>
          </div>
        </div>
      )}

      {/* Draw Offer Modal */}
      {showDrawModal && !isGameOver && (
        <div className="game-modal-overlay">
          <div className="game-modal">
            <h3>Opponent offered a draw</h3>
            <div className="modal-actions">
              <button className="btn-accept" onClick={() => onRespondDraw(true)}>Accept</button>
              <button className="btn-decline" onClick={() => onRespondDraw(false)}>Decline</button>
            </div>
          </div>
        </div>
      )}

      {/* Top Player Bar (Opponent) */}
      <div className={`player-bar top-bar ${topPlayerActive ? 'active-turn' : ''}`}>
        <div className="player-info-section">
          <Player username="Opponent" rating="?" />
        </div>
        {topPlayerActive && <div className="turn-indicator">Opponent's Turn</div>}
        <Clock
          initialSeconds={isWhite ? blackTime : whiteTime}
          isActive={topPlayerActive}
          lastMoveTimestamp={lastMoveTimestamp}
        />
      </div>

      {/* Chess Board Area */}
      <div className="board-wrapper">
        <Board addMove={addMove} fen={fen} userColor={userColor} />
      </div>

      {/* Bottom Player Bar (User) */}
      <div className={`player-bar bottom-bar ${bottomPlayerActive ? 'active-turn' : ''}`}>
        <div className="player-info-section">
          <Player username="You" rating="1200" />
        </div>
        {bottomPlayerActive && <div className="turn-indicator">Your Turn</div>}
        <Clock
          initialSeconds={isWhite ? whiteTime : blackTime}
          isActive={bottomPlayerActive}
          lastMoveTimestamp={lastMoveTimestamp}
        />
      </div>

      {/* Subtle Game Status (if needed, e.g., 'Check') - Optional, kept minimal */}
      {/* {statusMessage && !isGameOver && <div className="status-toast">{statusMessage}</div>} */}

    </div>
  );
};

export default BoardLayout;

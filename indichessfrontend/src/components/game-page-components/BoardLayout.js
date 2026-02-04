import React from "react";
// import { useState } from "react";
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
  const isSpectator = !userColor;

  // Modal logic
  const showDrawModal = drawOfferedBy && String(drawOfferedBy) !== String(userId) && drawOfferedBy !== "REJECTED";

  return (
    <div className="board-layout-main">

      {/* Draw Offer Modal */}
      {showDrawModal && (
        <div style={{
          position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%, -50%)',
          background: '#333', padding: '20px', border: '1px solid #777', zIndex: 100, color: 'white',
          borderRadius: '8px', boxShadow: '0 4px 6px rgba(0,0,0,0.3)', width: '300px', textAlign: 'center'
        }}>
          <h3 style={{ marginTop: 0 }}>Opponent offered a draw</h3>
          <div style={{ display: 'flex', gap: '10px', justifyContent: 'center', marginTop: '20px' }}>
            <button onClick={() => onRespondDraw(true)} style={{ background: 'green', color: 'white', padding: '8px 16px', border: 'none', cursor: 'pointer', borderRadius: '4px', fontWeight: 'bold' }}>Accept</button>
            <button onClick={() => onRespondDraw(false)} style={{ background: 'red', color: 'white', padding: '8px 16px', border: 'none', cursor: 'pointer', borderRadius: '4px', fontWeight: 'bold' }}>Decline</button>
          </div>
        </div>
      )}

      <div className="player-area top">
        {/* Opponent's Clock */}
        <Clock
          initialSeconds={isWhite ? blackTime : whiteTime}
          isActive={isWhite ? !isWhiteTurn : isWhiteTurn}
          lastMoveTimestamp={lastMoveTimestamp}
        />
        <Player />
      </div>

      <div className="game-status-bar" style={{
        color: 'white',
        textAlign: 'center',
        padding: '10px',
        backgroundColor: '#333',
        width: '100%',
        marginBottom: '5px',
        fontWeight: 'bold',
        fontSize: '1.1em'
      }}>
        {statusMessage}
      </div>

      <Board addMove={addMove} fen={fen} userColor={userColor} />

      <div className="player-area bottom">
        {/* User's Clock */}
        <Clock
          initialSeconds={isWhite ? whiteTime : blackTime}
          isActive={isWhite ? isWhiteTurn : !isWhiteTurn}
          lastMoveTimestamp={lastMoveTimestamp}
        />
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <Player />

          {/* Game Controls */}
          {!isSpectator && (
            <div className="game-controls" style={{ marginLeft: '20px', display: 'flex', gap: '10px' }}>
              <button onClick={onResign} style={{ background: '#444', color: '#fff', border: 'none', padding: '6px 12px', cursor: 'pointer', borderRadius: '4px' }}>
                Resign
              </button>
              <button onClick={onOfferDraw} style={{ background: '#444', color: '#fff', border: 'none', padding: '6px 12px', cursor: 'pointer', borderRadius: '4px' }}>
                Draw
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default BoardLayout;

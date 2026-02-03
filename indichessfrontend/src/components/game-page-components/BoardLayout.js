import React from "react";
// import { useState } from "react";
import Player from "./Player";
import Board from "./Board";
import "../component-styles/BoardLayout.css";

const BoardLayout = ({ addMove, fen, userColor, statusMessage }) => {



  return (
    <div className="board-layout-main">
      <Player />
      <div className="game-status-bar" style={{
        color: 'white',
        textAlign: 'center',
        padding: '10px',
        backgroundColor: '#333',
        width: '100%',
        marginBottom: '5px',
        fontWeight: 'bold'
      }}>
        {statusMessage}
      </div>
      <Board addMove={addMove} fen={fen} userColor={userColor} />
      <Player />
    </div>

  );
};

export default BoardLayout;

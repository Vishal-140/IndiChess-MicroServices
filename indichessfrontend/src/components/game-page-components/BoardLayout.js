import React from "react";
// import { useState } from "react";
import Player from "./Player";
import Board from "./Board";
import "../component-styles/BoardLayout.css";

const BoardLayout = ({ addMove, fen, userColor }) => {



  return (
    <div className="board-layout-main">
      <Player />
      <Board addMove={addMove} fen={fen} userColor={userColor} />
      <Player />
    </div>
  );
};

export default BoardLayout;

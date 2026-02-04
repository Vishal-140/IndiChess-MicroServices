import React, { useEffect, useRef } from "react";
import "../component-styles/GamePlayControlContainer.css";

const GamePlayControlContainer = ({ moves, onResign, onOfferDraw }) => {
  const movesEndRef = useRef(null);

  const scrollToBottom = () => {
    movesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [moves]);

  return (
    <div className="game-play-control-container">
      <div className="moves-header">
        <h3>Moves</h3>
      </div>

      <div className="moves-list">
        <table className="moves-table">
          <thead>
            <tr>
              <th>#</th>
              <th>White</th>
              <th>Black</th>
            </tr>
          </thead>
          <tbody>
            {moves.map((move, index) => (
              <tr key={index}>
                <td className="move-number">{index + 1}</td>
                <td className="move-white">{move.moveToWhite}</td>
                <td className="move-black">{move.moveToBlack || ""}</td>
              </tr>
            ))}
            <tr ref={movesEndRef} />
          </tbody>
        </table>
      </div>

      <div className="game-actions">
        <button onClick={onResign} className="action-btn resign-btn">
          Resign
        </button>
        <button onClick={onOfferDraw} className="action-btn draw-btn">
          Draw
        </button>
      </div>
    </div>
  );
};

export default GamePlayControlContainer;

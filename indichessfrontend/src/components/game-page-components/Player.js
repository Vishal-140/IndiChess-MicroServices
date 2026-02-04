import React from "react";
import { FaFlag } from "react-icons/fa";  // Use react-icons for flag icon (if needed)
import "../component-styles/Player.css";

const Player = ({ username, rating, country, time }) => {
  return (
    <div className="player-info-container">
      <div className="player-details">
        <span className="player-name">{username}</span>
        <span className="player-rating">({rating || "1200"})</span>
        {/* <span className="player-country">{country}</span> Flag removed */}
      </div>

      {/* Timer moved out or handled by parent if needed, but keeping structure minimal */}
      <div className="player-timer-display">
        {time}
      </div>
    </div>
  );
};

export default Player;

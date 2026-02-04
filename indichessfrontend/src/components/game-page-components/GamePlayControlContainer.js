import React, { useState } from "react";
import Analysis from "./Analysis";
import NewGame from "./NewGame";
import GamesPlayed from "./GamesPlayed"
import Players from "./Players";
import "../component-styles/GamePlayControlContainer.css";

import { useNavigate } from "react-router-dom";

const GamePlayControlContainer = ({ moves }) => {
  const [activeTab, setActiveTab] = useState("Analysis");
  const navigate = useNavigate();

  // Handle tab selection
  const handleTabClick = (tab) => {
    setActiveTab(tab);
  };

  const handleLogout = () => {
    localStorage.clear();
    navigate("/");
  };

  return (
    <div className="game-play-control-container">
      {/* Navigation Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', padding: '10px', background: '#222', borderBottom: '1px solid #444' }}>
        <button onClick={() => navigate("/")} style={{ background: 'transparent', color: '#aaa', border: 'none', cursor: 'pointer' }}>Home</button>
        <button onClick={handleLogout} style={{ background: 'transparent', color: '#faaa', border: 'none', cursor: 'pointer' }}>Logout</button>
      </div>

      {/* Header Section with Tabs */}
      <div className="header">
        <div
          className={`tab ${activeTab === "Analysis" ? "active" : ""}`}
          onClick={() => handleTabClick("Analysis")}
        >
          Analysis
        </div>
        <div
          className={`tab ${activeTab === "NewGame" ? "active" : ""}`}
          onClick={() => handleTabClick("NewGame")}
        >
          New Game
        </div>
        <div
          className={`tab ${activeTab === "Games" ? "active" : ""}`}
          onClick={() => handleTabClick("Games")}
        >
          Games
        </div>
        <div
          className={`tab ${activeTab === "Players" ? "active" : ""}`}
          onClick={() => handleTabClick("Players")}
        >
          Players
        </div>
      </div>

      {/* Content Section based on active tab */}
      <div className="content">
        {activeTab === "Analysis" && <Analysis moves={moves} />}
        {activeTab === "NewGame" && <NewGame />}
        {activeTab === "Games" && <GamesPlayed />}
        {activeTab === "Players" && <Players />}
      </div>
    </div>
  );
};

export default GamePlayControlContainer;

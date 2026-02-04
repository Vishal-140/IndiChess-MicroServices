import React, { useState, useRef, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { FaFire, FaRegHandshake, FaStopwatch, FaBolt, FaChessPawn, FaTimes } from "react-icons/fa";
import "../component-styles/GameInfo.css";

const GameInfo = ({ streak }) => {
  const navigate = useNavigate();
  const [isSearching, setIsSearching] = useState(false);
  const [searchTime, setSearchTime] = useState(0);
  const [gameType, setGameType] = useState("STANDARD"); // Default

  const pollingIntervalRef = useRef(null);
  const searchTimerRef = useRef(null);

  // Configuration for Game Modes
  const GAME_MODES = [
    {
      id: "BLITZ",
      title: "Blitz",
      time: "3 min + 2 sec",
      desc: "Fast paced action for quick thinkers.",
      icon: <FaBolt size={24} />
    },
    {
      id: "RAPID",
      title: "Rapid",
      time: "10 min",
      desc: "Balanced speed for tactical play.",
      icon: <FaStopwatch size={24} />
    },
    {
      id: "STANDARD",
      title: "Standard",
      time: "Unlimited",
      desc: "No timer. Pure chess strategy.",
      icon: <FaChessPawn size={24} />
    }
  ];

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      if (pollingIntervalRef.current) clearInterval(pollingIntervalRef.current);
      if (searchTimerRef.current) clearTimeout(searchTimerRef.current);
    };
  }, []);

  const getUserId = () => {
    let userId = localStorage.getItem("userId");
    if (!userId) {
      userId = Math.floor(Math.random() * 100000) + 1;
      localStorage.setItem("userId", userId);
    }
    return userId;
  };

  const cancelSearch = async () => {
    if (pollingIntervalRef.current) {
      clearInterval(pollingIntervalRef.current);
      pollingIntervalRef.current = null;
    }
    if (searchTimerRef.current) {
      clearTimeout(searchTimerRef.current);
      searchTimerRef.current = null;
    }

    try {
      await fetch(`http://localhost:8060/matchmaking/cancel?gameType=${gameType}`, {
        method: 'POST',
        headers: { 'X-USER-ID': getUserId() },
        credentials: 'include'
      });
    } catch (error) {
      console.error("Error cancelling search:", error);
    }

    setIsSearching(false);
    setSearchTime(0);
  };

  const pollForMatch = () => {
    let attempts = 0;
    const maxAttempts = 90;
    const userId = getUserId();

    pollingIntervalRef.current = setInterval(async () => {
      attempts++;
      setSearchTime(attempts);

      if (attempts >= maxAttempts) {
        cancelSearch();
        alert("Could not find an opponent. Please try again.");
        return;
      }

      try {
        const response = await fetch(`http://localhost:8060/matchmaking/check?gameType=${gameType}`, {
          method: 'GET',
          headers: { 'X-USER-ID': userId },
          credentials: 'include'
        });

        if (response.ok) {
          const result = await response.json();
          if (result.matchId && result.matchId > 0) {
            clearInterval(pollingIntervalRef.current);
            clearTimeout(searchTimerRef.current);
            setIsSearching(false);
            navigate(`/game?id=${result.matchId}`);
          } else if (result.matchId === -2) {
            cancelSearch(); // Timeout
          }
        }
      } catch (error) {
        console.error("Polling error:", error);
      }
    }, 1000);
  };

  const handlePlayClick = async () => {
    if (isSearching) {
      cancelSearch();
      return;
    }

    // Direct Game Creation Strategy to fix "Same ID" bug and ensure fresh state
    const userId = getUserId();
    const opponentId = Math.floor(Math.random() * 100000) + 1000; // Mock opponent for testing

    try {
      setIsSearching(true);
      // Using manual game creation endpoint
      const response = await fetch(`http://localhost:8060/games?gameType=${gameType}`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-PLAYER1-ID": userId,
          "X-PLAYER2-ID": String(opponentId),
          "X-USER-ID": userId // Some endpoints might check this
        },
        credentials: "include"
      });

      if (response.ok) {
        const game = await response.json();
        if (game.id) {
          setIsSearching(false);
          navigate(`/game?id=${game.id}`);
        } else {
          alert("Failed to create game: No ID returned");
          setIsSearching(false);
        }
      } else {
        const err = await response.text();
        console.error("Create Game failed:", err);
        alert("Failed to create new game. Ensure Backend is restarted.");
        setIsSearching(false);
      }
    } catch (e) {
      console.error("Create Game exception:", e);
      alert("Error creating game. Is the backend running?");
      setIsSearching(false);
    }
  };

  return (
    <div className="game-info">
      {/* Game Mode Selection Grid */}
      <h2 style={{ marginBottom: '30px', textAlign: 'center', color: '#E0E0E0', fontSize: '2rem', fontWeight: '600', letterSpacing: '1px' }}>Select Game Mode</h2>
      <div className="gamemode-grid">
        {GAME_MODES.map((mode) => (
          <div
            key={mode.id}
            className={`gamemode-card ${gameType === mode.id ? 'selected' : ''}`}
            onClick={() => !isSearching && setGameType(mode.id)}
          >
            <div className="mode-icon">{mode.icon}</div>
            <h4 className="mode-title">{mode.title}</h4>
            <span className="mode-time">{mode.time}</span>
            <p className="mode-desc">{mode.desc}</p>
          </div>
        ))}
      </div>

      {/* Play Button */}
      <div className="play-action-area">
        <button
          className={`play-btn ${isSearching ? 'cancel' : 'start'}`}
          onClick={handlePlayClick}
        >
          {isSearching ? (
            <>
              <FaTimes /> Cancel Search
            </>
          ) : (
            <>
              <FaRegHandshake /> Play {gameType}
            </>
          )}
        </button>
      </div>

      {/* Searching Status */}
      {isSearching && (
        <div className="searching-status">
          <div className="spinner"></div> {/* Use your existing spinner CSS if available, or simple text for now */}
          <p>Looking for a {gameType} match...</p>
          <p>{searchTime}s elapsed</p>
        </div>
      )}
    </div>
  );
};

export default GameInfo;
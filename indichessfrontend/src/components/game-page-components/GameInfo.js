import React, { useState, useRef, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { FaFire, FaRegHandshake, FaRobot, FaChessPawn, FaTimes } from "react-icons/fa";
import "../component-styles/GameInfo.css";

const GameInfo = ({ streak }) => {
  const navigate = useNavigate();
  const [isSearching, setIsSearching] = useState(false);
  const [searchTime, setSearchTime] = useState(0);
  const pollingIntervalRef = useRef(null);
  const searchTimerRef = useRef(null);

  // Clean up on unmount
  useEffect(() => {
    return () => {
      if (pollingIntervalRef.current) clearInterval(pollingIntervalRef.current);
      if (searchTimerRef.current) clearTimeout(searchTimerRef.current);
    };
  }, []);

  // Helper to get or create UserID
  const getUserId = () => {
    let userId = localStorage.getItem("userId");
    if (!userId) {
      userId = Math.floor(Math.random() * 100000) + 1; // Random ID for demo
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
    
    // Notify backend to remove from waiting queue
    try {
      await fetch(`http://localhost:8060/matchmaking/cancel?gameType=STANDARD`, {
        method: 'POST',
        headers: {
            'X-USER-ID': getUserId()
        }
      });
    } catch (error) {
      console.error("Error cancelling search:", error);
    }
    
    setIsSearching(false);
    setSearchTime(0);
  };

  const pollForMatch = () => {
    let attempts = 0;
    const maxAttempts = 90; // 90 seconds
    const userId = getUserId();
    
    pollingIntervalRef.current = setInterval(async () => {
      attempts++;
      setSearchTime(attempts);
      
      if (attempts >= maxAttempts) {
        cancelSearch();
        alert("Could not find an opponent within 90 seconds. Please try again.");
        return;
      }
      
      try {
        const response = await fetch(`http://localhost:8060/matchmaking/check?gameType=STANDARD`, {
          method: 'GET',
          headers: {
            'X-USER-ID': userId
          }
        });

        if (response.ok) {
          const result = await response.json();
          console.log("Poll result:", result);
          
          if (result.matchId && result.matchId > 0) {
            // Match found! Stop polling and redirect
            clearInterval(pollingIntervalRef.current);
            pollingIntervalRef.current = null;
            clearTimeout(searchTimerRef.current);
            searchTimerRef.current = null;
            
            setIsSearching(false);
            setSearchTime(0);
            navigate(`/game?id=${result.matchId}`); // Navigate with Query Param
          } else if (result.matchId === -2) {
            // Error case / Timeout
            cancelSearch();
          }
           // If matchId === -1, continue waiting
        }
      } catch (error) {
        console.error("Error polling for match:", error);
      }
    }, 1000); // Poll every second
  };

  const createNewGame = async () => {
    if (isSearching) {
      cancelSearch();
      return;
    }

    setIsSearching(true);
    setSearchTime(0);
    const userId = getUserId();
    
    try {
      const response = await fetch(`http://localhost:8060/matchmaking/join?gameType=STANDARD`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-USER-ID': userId
        }
      });

      if (response.ok) {
        const result = await response.json();
        console.log("Create game response:", result);
        
        if (result.matchId === -1) {
          // Waiting for opponent, start polling
          pollForMatch();
          
          // Set timeout for 90 seconds
          searchTimerRef.current = setTimeout(() => {
            if (isSearching) {
              cancelSearch();
              alert("Could not find an opponent within 90 seconds. Please try again.");
            }
          }, 90000);
          
        } else if (result.matchId > 0) {
          // Match found immediately (e.g. was already in queue or instant match)
          setIsSearching(false);
          navigate(`/game?id=${result.matchId}`);
        } else {
          setIsSearching(false);
          alert("Failed to join queue. Status: " + result.matchId);
        }
      } else {
        setIsSearching(false);
        alert("Failed to create match. Server error.");
      }
    } catch (error) {
      console.error("Error creating game:", error);
      setIsSearching(false);
    }
  };

  return (
    <div className="game-info">
      {/* Streak Section */}
      <div className="streak">
        <FaFire size={30} />
        <div>
          <p>Streak</p>
          <h3>{streak} Days</h3>
        </div>
      </div>

      {/* Buttons Section */}
      <div className="buttons">
        <button className="button">
          <FaChessPawn size={20} />
          Play 1 | 1
        </button>
        <button 
          className={`button ${isSearching ? 'searching' : ''}`} 
          onClick={createNewGame}
        >
          {isSearching ? (
            <>
              <FaTimes size={20} />
              Cancel ({searchTime}s)
            </>
          ) : (
            <>
              <FaChessPawn size={20} />
              New Game
            </>
          )}
        </button>
        <button className="button">
          <FaRobot size={20} />
          Play Bots
        </button>
        <button className="button">
          <FaRegHandshake size={20} />
          Play a Friend
        </button>
      </div>
      
      {/* Searching indicator */}
      {isSearching && (
        <div className="searching-indicator">
          <div className="spinner"></div>
          <p>Searching for opponent... {searchTime}s</p>
          <p className="searching-hint">(Wait for another player to click "New Game")</p>
        </div>
      )}
    </div>
  );
};

export default GameInfo;
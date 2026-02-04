import React from 'react';
import { useNavigate } from 'react-router-dom';
import "./component-styles/SideNav.css";
import { FaChessPawn, FaCog, FaLifeRing, FaSignOutAlt } from 'react-icons/fa';

const SideNav = () => {
  const navigate = useNavigate();

  const handleLogout = async () => {
    // Check if we are in a game and Resign first
    const searchParams = new URLSearchParams(window.location.search);
    const currentGameId = searchParams.get('id');
    const userId = localStorage.getItem("userId");

    if (currentGameId && userId) {
      try {
        console.log(`Resigning game ${currentGameId} due to logout...`);
        await fetch(`http://localhost:8060/games/${currentGameId}/resign`, {
          method: "POST",
          headers: { "X-USER-ID": userId, "Content-Type": "application/json" },
          credentials: "include"
        });
      } catch (e) {
        console.error("Auto-resign failed on logout", e);
      }
    }

    try {
      await fetch("http://localhost:8060/logout", {
        method: "POST",
        credentials: "include"
      });
    } catch (e) {
      console.error("Logout failed at backend", e);
    }
    localStorage.clear();
    navigate("/");
  };

  return (
    <div className="side-nav">
      <div className="logo">
        <FaChessPawn size={28} />
        <h2>IndiChess</h2>
      </div>

      <div className="menu">
        <button className="menu-item active">
          <FaChessPawn size={20} />
          <span>Play</span>
        </button>
      </div>

      <div className="settings">
        <button className="settings-item">
          <FaCog size={20} />
          <span>Settings</span>
        </button>
        <button className="settings-item">
          <FaLifeRing size={20} />
          <span>Support</span>
        </button>
        <button className="settings-item" onClick={handleLogout} style={{ color: 'var(--accent-red)' }}>
          <FaSignOutAlt size={20} />
          <span>Logout</span>
        </button>
      </div>
    </div>
  );
};

export default SideNav;

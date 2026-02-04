import React from "react";
import { FaUser, FaBell } from "react-icons/fa";
import "./component-styles/Header.css";

const Header = () => {
  const username = localStorage.getItem("username") || "Player";

  return (
    <div className="header">
      <div className="header-left">
        <h3>Welcome back, <span className="username-highlight">{username}</span></h3>
      </div>

      <div className="header-right">
        <FaBell size={20} className="header-icon" title="Notifications" />
        <FaUser size={20} className="header-icon" title="Profile" />
      </div>
    </div>
  );
};

export default Header;

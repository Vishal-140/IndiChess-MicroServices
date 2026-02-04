import React from "react";
import SideNav from "../components/SideNav";
import Header from "../components/Header";
import GameInfo from "../components/game-page-components/GameInfo";
import "./Home.css"; // Global layout styles

function HomePage() {
  return (
    <div className="app-container">
      <SideNav />

      <div className="main-content">
        <Header />

        <div className="content-wrapper">
          <GameInfo />
        </div>
      </div>
    </div>
  );
}

export default HomePage;

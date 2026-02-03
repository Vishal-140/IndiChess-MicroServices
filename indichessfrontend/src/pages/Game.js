import Header from "../components/Header";
import SideNav from "../components/SideNav";
import GameContainer from "../components/game-page-components/GameContainer";


const Game = () => {
  
  return (
    <div className="app-container">
      <SideNav /> {/* Render the SideNav */}
      <div className="main-container">
        <Header />
        <GameContainer />
      </div>
    </div>
  );
};

export default Game;

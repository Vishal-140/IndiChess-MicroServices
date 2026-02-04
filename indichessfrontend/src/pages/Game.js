import Header from "../components/Header";
import SideNav from "../components/SideNav";
import GameContainer from "../components/game-page-components/GameContainer";


const Game = () => {

  return (
    <div className="app-container">
      <SideNav /> {/* Render the SideNav */}
      <div className="main-content" style={{ marginLeft: 'var(--sidebar-width)', width: '100%' }}>
        <Header />
        <div style={{ padding: '10px', height: 'calc(100vh - 60px)', boxSizing: 'border-box', overflow: 'hidden' }}>
          <GameContainer />
        </div>
      </div>
    </div>
  );
};

export default Game;

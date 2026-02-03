import { BrowserRouter as Router, Route, Routes } from "react-router-dom";
import LoginCard from './components/LoginCard';
import SignupCard from "./components/SignUpCard";
import HomeCard from "./pages/HomeCard";
import HomePage from "./pages/Home";
import './App.css';
import Game from "./pages/Game";


import { RequireAuth, RedirectIfAuthenticated } from "./components/auth/AuthWrapper";

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={
          <RedirectIfAuthenticated>
            <HomeCard />
          </RedirectIfAuthenticated>
        } />
        <Route path="/home" element={
          <RequireAuth>
            <HomePage />
          </RequireAuth>
        } />
        <Route path="/game" element={
          <RequireAuth>
            <Game />
          </RequireAuth>
        } />
      </Routes>
    </Router>
  );
}

export default App;

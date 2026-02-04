import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import LoginCard from '../components/LoginCard';
import SignupCard from '../components/SignUpCard';

function HomeCard() {
  const [showSignup, setShowSignup] = useState(false); // Track if we need to show SignupCard
  const [isAuthenticated, setIsAuthenticated] = useState(false); // To track authentication status
  const navigate = useNavigate(); // For navigation (redirecting to home)

  // Check if the user is already authenticated on component mount
  useEffect(() => {
    const checkAuth = async () => {
      try {
        const response = await fetch("http://localhost:8060/home", {
          method: "GET",
          credentials: "include",
        });

        if (response.ok) {
          // If authenticated, we MUST check if we have local storage userId.
          if (localStorage.getItem("userId")) {
            setIsAuthenticated(true);
            navigate("/home");
          } else {
            // Zombie Cookie Case: Backend says OK, but Frontend has no user.
            // We must logout to clear the cookie and stay here.
            console.warn("Detected zombie cookie (Auth OK but no local user). Clearing...");
            await fetch("http://localhost:8060/logout", { method: "POST", credentials: "include" });
            setIsAuthenticated(false);
          }
        } else {
          setIsAuthenticated(false);
        }
      } catch (error) {
        console.error("Error checking authentication:", error);
        setIsAuthenticated(false);
      }
    };

    checkAuth();
  }, [navigate]);

  const handleToggleSignup = () => {
    setShowSignup((prev) => !prev); // Toggle between login and signup
  };

  return (
    <div className="home-card">
      {isAuthenticated ? (
        // If authenticated, don't show the login/signup cards, just redirect to /home
        <div>Redirecting to Home...</div>
      ) : (
        <div>
          {showSignup ? (
            <SignupCard handleToggleSignup={handleToggleSignup} />
          ) : (
            <LoginCard handleToggleSignup={handleToggleSignup} />
          )}
        </div>
      )}
    </div>
  );
}

export default HomeCard;

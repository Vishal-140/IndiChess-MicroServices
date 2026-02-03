import React from 'react';
import { Navigate } from 'react-router-dom';

export const RequireAuth = ({ children }) => {
    const userId = localStorage.getItem("userId");

    if (!userId) {
        // Redirect them to the login page, but save the current location they were
        // trying to go to when they were redirected. This allows us to send them
        // along to that page after they login, which is a nicer user experience.
        return <Navigate to="/" replace />;
    }

    return children;
};

export const RedirectIfAuthenticated = ({ children }) => {
    const userId = localStorage.getItem("userId");

    if (userId) {
        return <Navigate to="/home" replace />;
    }

    return children;
};

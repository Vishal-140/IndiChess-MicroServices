import React, { useState, useEffect, useRef } from 'react';
import '../component-styles/Clock.css';

const Clock = ({ initialSeconds, isActive, lastMoveTimestamp }) => {
    const [secondsRemaining, setSecondsRemaining] = useState(initialSeconds);
    const intervalRef = useRef(null);

    useEffect(() => {
        // If we have a timestamp for the last move, calculate the accurate remaining time
        // But be careful: lastMoveTimestamp is for the *game*'s last move.
        // We only want to adjust if THIS clock is active.
        if (isActive && lastMoveTimestamp && initialSeconds > 0) {
            const lastMoveTime = new Date(lastMoveTimestamp).getTime();
            const now = new Date().getTime();
            const diffInSeconds = Math.floor((now - lastMoveTime) / 1000);
            // Sync: The initialSeconds passed from parent is the time *at the moment of the last move*.
            // So we subtract the elapsed time since then.
            setSecondsRemaining(Math.max(0, initialSeconds - diffInSeconds));
        } else {
            setSecondsRemaining(initialSeconds);
        }
    }, [initialSeconds, isActive, lastMoveTimestamp]);

    useEffect(() => {
        if (isActive && secondsRemaining > 0) {
            intervalRef.current = setInterval(() => {
                setSecondsRemaining((prev) => Math.max(0, prev - 1));
            }, 1000);
        } else {
            clearInterval(intervalRef.current);
        }

        return () => clearInterval(intervalRef.current);
    }, [isActive, secondsRemaining]);

    const formatTime = (secs) => {
        if (secs === null || secs === undefined) return "--:--";
        const minutes = Math.floor(secs / 60);
        const seconds = secs % 60;
        return `${minutes}:${seconds < 10 ? '0' : ''}${seconds}`;
    };

    return (
        <div className={`chess-clock ${isActive ? 'active' : ''} ${secondsRemaining < 30 ? 'low-time' : ''}`}>
            {formatTime(secondsRemaining)}
        </div>
    );
};

export default Clock;

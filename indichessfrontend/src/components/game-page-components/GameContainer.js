import React, { useState, useEffect, useRef } from "react";
import BoardLayout from "./BoardLayout";
import GamePlayControlContainer from "./GamePlayControlContainer";
import { useSearchParams } from "react-router-dom";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

const GameContainer = () => {
  const [searchParams] = useSearchParams();
  const gameId = searchParams.get("id");
  const [moves, setMoves] = useState([]);
  const [fen, setFen] = useState(""); // Backend FEN
  const stompClientRef = useRef(null);
  const userId = localStorage.getItem("userId") || "0";
  const [statusMessage, setStatusMessage] = useState("Connecting...");
  const [whiteTime, setWhiteTime] = useState(null);
  const [blackTime, setBlackTime] = useState(null);
  const [lastMoveTimestamp, setLastMoveTimestamp] = useState(null);
  const [isWhiteTurn, setIsWhiteTurn] = useState(true);
  const [drawOfferedBy, setDrawOfferedBy] = useState(null); // userId of offerer

  const [userColor, setUserColor] = useState(null); // 'w' or 'b'

  // Define fetchGameDetails at component level so it can be reused
  const fetchGameDetails = async () => {
    if (!gameId) return;
    try {
      const response = await fetch(`http://localhost:8060/games/${gameId}`, {
        headers: { "X-USER-ID": userId },
        credentials: "include"
      });
      if (response.ok) {
        const data = await response.json();
        console.log("Game details fetched:", data);

        // Calculate turn first
        const currentPly = data.currentPly || 0;
        const isWhiteTurnNow = currentPly % 2 === 0;
        setIsWhiteTurn(isWhiteTurnNow);

        // data.player1Id is White, data.player2Id is Black
        const p1 = String(data.player1Id);
        const p2 = String(data.player2Id);
        const uId = String(userId);
        const isSelfPlay = (p1 === uId && p2 === uId);

        if (isSelfPlay) {
          setUserColor(isWhiteTurnNow ? 'w' : 'b');
        } else if (p1 === uId) {
          setUserColor('w');
        } else if (p2 === uId) {
          setUserColor('b');
        } else {
          setUserColor('spectator');
        }

        if (data.fenCurrent) setFen(data.fenCurrent);

        if (data.status && data.status !== "IN_PROGRESS") {
          setStatusMessage(`Game Over: ${data.status}`);
        } else {
          let msg = "Spectating";
          if (isSelfPlay) {
            msg = isWhiteTurnNow ? "Your Turn (White)" : "Your Turn (Black)";
          } else if (p1 === uId) {
            msg = isWhiteTurnNow ? "Your Turn" : "Opponent's Turn";
          } else if (p2 === uId) {
            msg = !isWhiteTurnNow ? "Your Turn" : "Opponent's Turn";
          }
          setStatusMessage(msg);
        }

        // Sync Timers
        if (data.whiteTime !== undefined) setWhiteTime(data.whiteTime);
        if (data.blackTime !== undefined) setBlackTime(data.blackTime);
        if (data.lastMoveTimestamp) setLastMoveTimestamp(data.lastMoveTimestamp);

        // Restore Moves
        if (data.moves && Array.isArray(data.moves)) {
          const restoredMoves = [];
          for (let i = 0; i < data.moves.length; i += 2) {
            const whiteMove = data.moves[i];
            const blackMove = data.moves[i + 1];

            const moveObj = {
              moveToWhite: whiteMove.san || whiteMove.uci,
              moveToBlack: blackMove ? (blackMove.san || blackMove.uci) : null,
            };
            restoredMoves.push(moveObj);
          }
          setMoves(restoredMoves);
        }
      }
    } catch (e) {
      console.error("Failed to fetch game details", e);
    }
  };

  useEffect(() => {
    // 1. Initial Fetch
    fetchGameDetails();

    // 2. Polling Fallback (Every 2 seconds)
    // This guarantees sync even if WebSocket connection fails or Gateway drops messages
    const intervalId = setInterval(() => {
      fetchGameDetails();
    }, 2000);

    // 3. WebSocket Setup (Real-time updates)
    const socket = new SockJS("http://localhost:8060/ws");
    const client = new Client({
      webSocketFactory: () => socket,
      connectHeaders: {
        "X-USER-ID": userId,
      },
      debug: (str) => console.log(str),
      onConnect: () => {
        console.log("Connected to WS");

        client.subscribe(`/topic/game/${gameId}`, (message) => {
          const body = JSON.parse(message.body);
          console.log("Received move:", body);

          if (body.fen) {
            setFen(body.fen);

            // Update Turn Status
            if (body.status && body.status !== "IN_PROGRESS") {
              setStatusMessage(`Game Over: ${body.status}`);
            } else {
              const nextTurn = body.nextTurn; // "WHITE" or "BLACK"
              const whiteTurn = nextTurn === 'WHITE';
              setIsWhiteTurn(whiteTurn);

              // Self-play heuristic sync handled by re-render
            }

            // Sync Time on Move
            if (body.whiteTime !== undefined) setWhiteTime(body.whiteTime);
            if (body.blackTime !== undefined) setBlackTime(body.blackTime);

            // Draw Offer
            if (body.drawOfferBy !== undefined) {
              setDrawOfferedBy(body.drawOfferBy);
            }

            setLastMoveTimestamp(new Date().toISOString());
          }
        });
      },
      onStompError: (frame) => {
        console.error("Broker reported error: " + frame.headers["message"]);
        console.error("Additional details: " + frame.body);
      },
    });

    try {
      client.activate();
    } catch (e) {
      console.error("WS Activation failed", e);
    }
    stompClientRef.current = client;

    return () => {
      clearInterval(intervalId); // Stop polling on unmount
      if (client.connected) {
        client.deactivate();
      }
    };
  }, [gameId, userId]);

  // Function to add a move (Triggered by Board.js drop)
  const addMove = async (move) => {
    // 1. Construct UCI
    let uci = move.moveFrom; // e.g. "e2"
    const destRank = 8 - move.tr;
    const destFile = String.fromCharCode('a'.charCodeAt(0) + move.tc);
    uci += destFile + destRank; // "e2e4"

    if (move.promotion) {
      uci += move.promotion.toLowerCase();
    }

    console.log("Sending Move (REST):", uci);

    // 2. Send via REST for reliability
    try {
      const response = await fetch(`http://localhost:8060/games/${gameId}/move`, {
        method: "POST",
        headers: {
          "X-USER-ID": userId,
          "Content-Type": "application/json"
        },
        credentials: "include",
        body: JSON.stringify({ uci: uci })
      });

      if (!response.ok) {
        const err = await response.text();
        console.error("Move failed:", err);
        alert("Invalid Move: " + err);
        // We might want to revert the board here, but Board.js state is complex.
        // For now, refreshing or fetching details will fix it.
        fetchGameDetails();
        return;
      } else {
        // Success
        console.log("Move sent successfully");
        // We rely on WebSocket for the "Turn Check" update, 
        // but we can also force fetch to be safe (fixes "Your Turn" not changing if WS lags)
        fetchGameDetails();
      }
    } catch (e) {
      console.error("Error sending move:", e);
      alert("Network Error sending move.");
    }

    // 3. Optimistic UI Update (Local)
    // This updates the visual board immediately for smoothness
    if (move.piece !== move.piece.toLowerCase()) {
      const newMove = { move, moveToWhite: move.moveTo };
      setMoves((moves) => [...moves, newMove]);
    }
    else {
      setMoves((prevMoves) => {
        const newMoves = [...prevMoves];
        if (newMoves.length > 0) {
          const lastMove = {
            ...newMoves[newMoves.length - 1],
            moveToBlack: move.moveTo,
            tc: `Black's Turn: ${move.tc}`,
            tr: move.tr
          };
          newMoves[newMoves.length - 1] = lastMove;
          return newMoves;
        } else {
          return [...prevMoves, { flow: "weird", moveToBlack: move.moveTo }];
        }
      });
    }
  };

  // --- ACTIONS ---
  const handleResign = async () => {
    if (!gameId || !userId) return;
    try {
      console.log("Resigning game...");
      const response = await fetch(`http://localhost:8060/games/${gameId}/resign`, {
        method: "POST",
        headers: { "X-USER-ID": userId, "Content-Type": "application/json" },
        credentials: "include"
      });
      if (!response.ok) {
        const errText = await response.text();
        console.error("Resign failed:", response.status, errText);
        alert(`Failed to resign: ${errText || response.statusText}`);
      } else {
        fetchGameDetails();
      }
    } catch (e) {
      console.error("Resign exception", e);
      alert("Error resigning game.");
    }
  };

  const handleOfferDraw = async () => {
    if (!gameId || !userId) return;
    try {
      console.log("Offering draw...");
      const response = await fetch(`http://localhost:8060/games/${gameId}/draw-offer`, {
        method: "POST",
        headers: { "X-USER-ID": userId, "Content-Type": "application/json" },
        credentials: "include"
      });
      if (!response.ok) {
        const errText = await response.text();
        console.error("Offer Draw failed:", response.status, errText);
        alert(`Failed to offer draw: ${errText || response.statusText}`);
      } else {
        alert("Draw offered successfully. Waiting for opponent...");
      }
    } catch (e) {
      console.error("Offer Draw exception", e);
      alert("Error offering draw.");
    }
  };

  const handleRespondDraw = async (accept) => {
    if (!gameId || !userId) return;
    try {
      console.log(`Responding to draw: ${accept}`);
      const response = await fetch(`http://localhost:8060/games/${gameId}/draw-response?accept=${accept}`, {
        method: "POST",
        headers: { "X-USER-ID": userId, "Content-Type": "application/json" },
        credentials: "include"
      });
      setDrawOfferedBy(null);
      if (!response.ok) {
        const errText = await response.text();
        console.error("Respond Draw failed:", response.status, errText);
        alert(`Failed to respond to draw: ${errText}`);
      } else {
        if (accept) fetchGameDetails();
      }
    } catch (e) {
      console.error("Respond Draw exception", e);
    }
  };

  return (
    <div className="game-container">
      <BoardLayout
        addMove={addMove}
        fen={fen}
        userColor={userColor}
        statusMessage={statusMessage}
        whiteTime={whiteTime}
        blackTime={blackTime}
        lastMoveTimestamp={lastMoveTimestamp}
        isWhiteTurn={isWhiteTurn}

        // Actions
        onResign={handleResign}
        onOfferDraw={handleOfferDraw}
        onRespondDraw={handleRespondDraw}
        drawOfferedBy={drawOfferedBy}
        userId={userId}
      />
      <GamePlayControlContainer
        moves={moves}
        onResign={handleResign}
        onOfferDraw={handleOfferDraw}
      />
    </div>
  );
};

export default GameContainer;

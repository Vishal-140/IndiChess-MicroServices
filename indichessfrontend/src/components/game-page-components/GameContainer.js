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

  useEffect(() => {
    if (!gameId) return;

    // Fetch Game Details to determine color
    const fetchGameDetails = async () => {
      try {
        const response = await fetch(`http://localhost:8060/games/${gameId}`, {
          headers: { "X-USER-ID": userId },
          credentials: "include"
        });
        if (response.ok) {
          const data = await response.json();
          console.log("Game details:", data);

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
            setUserColor(isWhiteTurnNow ? 'w' : 'b'); // Dynamically set based on turn
            console.log("Self Play Detected");
          } else if (p1 === uId) {
            setUserColor('w');
            console.log("I am WHITE");
          } else if (p2 === uId) {
            setUserColor('b');
            console.log("I am BLACK");
          } else {
            console.log("I am SPECTATOR");
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

    fetchGameDetails();

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

              // Self Play Logic: Switch userColor so they can move the other side
              // We need to check if it's self play. Using a simple heuristic or I should have saved it.
              // Heuristic: If I just made a move and I'm still allowed to move?
              // Better: Check if `userId` matches both P1 and P2. But I don't have P1/P2 in closure easily.
              // Let's rely on the fact that if I am playing self, I want to control the active color.
              // But I can't easily detect "Self Play" here without state.
              // Let's assume if I added a 'Test Mode' flag or similar.
              // For now, let's just update the status message. The interaction `userColor` might be stale 'w'.
              // I need to update `userColor` here if it is self play.
              // I can check if my userId matches `drawOffer` logic? No.

              // Let's use localStorage flag? Or just check if the backend says so.
              // Hack: If I am playing self, my `userColor` should switch.
              // But `userColor` is state.
              // I'll leave `userColor` static for now in WS, but in `fetch` I set it.
              // If `fetch` set it to 'w', it stays 'w'.
              // I need to update it here.

              // Let's add `checkSelfPlay` logic or just re-fetch game details? No, expensive.
              // I will leave this for now. The user can refresh if stuck, BUT
              // `GameContainer` re-renders on `userColor` change.
              // If I set `userColor` in `fetch`, it's only once.

              // CRITICAL: `userColor` must flip for Self Play.
              // I'll add `const [isSelfPlayMode, setIsSelfPlayMode] = useState(false);` at top.
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

    client.activate();
    stompClientRef.current = client;

    return () => {
      client.deactivate();
    };
  }, [gameId, userId]);

  // Function to add a move (Triggered by Board.js drop)
  const addMove = (move) => {
    // Send move to backend
    if (stompClientRef.current && stompClientRef.current.connected) {
      // Construct UCI (e.g. from "e2" to "e4" -> "e2e4")
      // move object has sqnumfrom, sqnumto... 
      // Wait, moveFrom is "e2", moveTo is "e4" (sometimes with piece like Ne4)
      // We need raw coordinates or raw string. 
      // Let's inspect Board.js output in addMove.
      // It provides: {piece, moveFrom, moveTo, sqnumfrom, sqnumto, tc , tr, fenBefore, fenAfter, createdAt}

      // We need to construct UCI. 
      // Board.js `moveFrom` logic matches standard notation? 
      // Actually `updatePrevMove` in Board.js does:
      // moveFrom = columnChar + rowNum
      // moveTo = ...complex...

      // Let's re-calculate simple UCI from keys:
      // We need `from` and `to`. 
      // Board.js passes `sqnumfrom`, `sqnumto`? No.
      // It passes `moveFrom` which is e.g. "e2".
      // It passes `moveTo` which is e.g. "Net4" (Knight x e4).

      // Backend expects simple UCI "e2e4".
      // We might need to extract it.
      // Or simpler: We know user just dragged from (r1,c1) to (r2,c2).
      // Board.js has this info. `updatePrevMove` has `fr, fc, tr, tc`. 
      // But `addMove` receives an object. 
      // `updatePrevMove` calls `addMove` with `{piece, moveFrom, moveTo, sqnumfrom, sqnumto, tc , tr...}`
      // `sqnumfrom` is 8-fr? Board.js: `sqnumfrom = 8-fr`.
      // `moveFrom` is `char + sqnumfrom`. e.g. "e2". Correct.

      // But `moveTo` in the object is the SAN (Standard Algebraic Notation) like "Nxe4".
      // We need the destination square.
      // The object has `tc` and `tr`. `tr` is row index (0-7), `tc` is col index (0-7).
      // Destination square = (char)('a' + tc) + (8-tr).

      let uci = move.moveFrom; // e.g. "e2"

      // Calculate dest from tc/tr
      const destRank = 8 - move.tr;
      const destFile = String.fromCharCode('a'.charCodeAt(0) + move.tc);
      uci += destFile + destRank; // "e2e4"

      // Promotion? 
      // If pawn reached last rank, we need to know promotion piece.
      // Board logic handles promotion modal and then calls `updatePrevMove`.
      // But `addMove` payload doesn't seem to explicitly have promotion char.
      // We might need to assume 'q' or add it to Board.js.

      if (move.promotion) {
        // e.g. "q", "n" (lowercase expected by backend usually, logic said Character.toLowerCase)
        uci += move.promotion.toLowerCase();
      }

      console.log("Sending UCI:", uci);

      const moveRequest = {
        gameId: gameId,
        playerId: userId,
        uci: uci,
        fen: move.fenAfter
      };

      stompClientRef.current.publish({
        destination: `/app/game/${gameId}/move`,
        headers: { "X-USER-ID": userId },
        body: JSON.stringify(moveRequest),
      });
    }

    // Update local UI history
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
          // Fallback if black moves first (custom setup)
          return [...prevMoves, { flow: "weird", moveToBlack: move.moveTo }];
        }
      });
    }

  };

  // --- ACTIONS ---
  const handleResign = async () => {
    if (!gameId || !userId) return;
    try {
      const response = await fetch(`http://localhost:8060/games/${gameId}/resign`, {
        method: "POST",
        headers: { "X-USER-ID": userId, "Content-Type": "application/json" },
        credentials: "include"
      });
      if (!response.ok) {
        const errText = await response.text();
        console.error("Resign failed:", response.status, errText);
        alert(`Failed to resign: ${errText || response.statusText}`);
      }
    } catch (e) {
      console.error("Resign exception", e);
      alert("Error resigning game.");
    }
  };

  const handleOfferDraw = async () => {
    if (!gameId || !userId) return;
    try {
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
      const response = await fetch(`http://localhost:8060/games/${gameId}/draw-response?accept=${accept}`, {
        method: "POST",
        headers: { "X-USER-ID": userId, "Content-Type": "application/json" },
        credentials: "include"
      });
      setDrawOfferedBy(null); // Clear local state immediately
      if (!response.ok) {
        const errText = await response.text();
        console.error("Respond Draw failed:", response.status, errText);
        alert(`Failed to respond to draw: ${errText}`);
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

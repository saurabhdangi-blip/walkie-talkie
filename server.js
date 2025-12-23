const express = require("express");
const path = require("path");
const WebSocket = require("ws");

const app = express();
const PORT = process.env.PORT || 8080;

/* Serve React build */
app.use(express.static(path.join(__dirname, "walkie-client", "build")));

app.get(/^\/(?!ws).*/, (req, res) => {
  res.sendFile(
    path.join(__dirname, "walkie-client", "build", "index.html")
  );
});


const server = app.listen(PORT, () => {
  console.log("🚀 Server running on", PORT);
});

/* ================== WEBSOCKET ================== */

const wss = new WebSocket.Server({ server });

/*
 users = {
   id: { ws, role }
 }
*/
const users = {};

wss.on("connection", (ws) => {

  ws.on("message", (msg) => {
    const data = JSON.parse(msg.toString());

    /* REGISTER */
    if (data.type === "register") {
      users[data.id] = { ws, role: data.role };
      ws.id = data.id;

      console.log("✅ Registered:", data.id, data.role);
      broadcastAndroidUsers();
      return;
    }

    /* SIGNALING */
    if (["offer", "answer", "ice", "call-ended"].includes(data.type)) {
      if (data.target && users[data.target]) {
        users[data.target].ws.send(JSON.stringify(data));
      } else {
        console.log("⚠️ Dropped", data.type, "no target");
      }
    }
  });

  ws.on("close", () => {
    delete users[ws.id];
    broadcastAndroidUsers();
    console.log("❌ Disconnected:", ws.id);
  });
});

/* SEND ANDROID IDS TO WEB CLIENTS */
function broadcastAndroidUsers() {
  const androidIds = Object.entries(users)
    .filter(([_, u]) => u.role === "android")
    .map(([id]) => id);

  Object.values(users).forEach(u => {
    if (u.role === "web") {
      u.ws.send(JSON.stringify({
        type: "android-users",
        users: androidIds
      }));
    }
  });
}

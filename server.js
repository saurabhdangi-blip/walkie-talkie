const express = require("express");
const path = require("path");
const WebSocket = require("ws");

const app = express();
const server = app.listen(8080, () => {
  console.log("🚀 Server running on http://localhost:8080");
});


console.log("🚀 WS running on ws://localhost:8080");

app.use(express.static(path.join(__dirname, "walkie-client", "build")));

app.get(/^\/(?!ws).*/, (req, res) => {
  res.sendFile(
    path.join(__dirname, "walkie-client", "build", "index.html")
  );
});

const wss = new WebSocket.Server({ server });
const users = {};

wss.on("connection", (ws) => {

  ws.on("message", (msg) => {
    const data = JSON.parse(msg.toString());

    if (data.type === "register") {
      users[data.id] = ws;
      ws.id = data.id;
      console.log("✅ Registered", data.id);
    }

    if (["offer", "answer", "ice"].includes(data.type)) {
      users[data.target]?.send(JSON.stringify(data));
    }
  });

  ws.on("close", () => {
    delete users[ws.id];
    console.log("❌ Disconnected", ws.id);
  });
});

const express = require("express");
const path = require("path");
const WebSocket = require("ws");

const app = express();
const PORT = process.env.PORT || 8080;


app.use(express.static(path.join(__dirname, "walkie-client", "build")));

app.get(/^\/(?!ws).*/, (req, res) => {
  res.sendFile(
    path.join(__dirname, "walkie-client", "build", "index.html")
  );
});

const server = app.listen(PORT, () => {
  console.log("🚀 Server running on port", PORT);
});

/* WebSocket */
const wss = new WebSocket.Server({ server });
const users = {};

wss.on("connection", (ws) => {
  ws.on("message", (msg) => {
    const data = JSON.parse(msg);

    if (data.type === "register") {
      users[data.id] = ws;
      ws.id = data.id;
      console.log("✅ Registered", data.id);
    }

    if (["offer", "answer", "ice", "call-ended"].includes(data.type)) {
      users[data.target]?.send(JSON.stringify(data));
    }
  });

  ws.on("close", () => {
    delete users[ws.id];
    console.log("❌ Disconnected", ws.id);
  });
});

// server.js
const express = require("express");
const http = require("http");
const { Server } = require("socket.io");
const app = express();
const server = http.createServer(app);
const io = new Server(server, { cors: { origin: "*" } });

const users = {};

io.on("connection", socket => {
  console.log("New connection:", socket.id);

  socket.on("register", userId => {
    users[userId] = socket.id;
    socket.userId = userId;
    console.log("Registered:", userId);
  });

  socket.on("audio", data => {
    const { to, buffer } = data;
    if (users[to]) {
      io.to(users[to]).emit("audio", buffer); // Base64 string
    }
  });

  socket.on("disconnect", () => {
    delete users[socket.userId];
  });
});

server.listen(3000, () => console.log("Server running on 3000"));

"use strict";

const connection =
    new signalR.HubConnectionBuilder()
        .withUrl("https://chathub-b2hdatgbenfca9f5.eastus2-01.azurewebsites.net/chatHub")
        .withAutomaticReconnect()
        .build();

const sendButton = document.getElementById("sendButton");
const status = document.getElementById("status");
const messagesList = document.getElementById("messagesList");
const roomInput = document.getElementById("roomInput");
const joinRoomButton = document.getElementById("joinRoomButton");

let salaUnida = null;

connection.on("ReceiveMessage", (message) => {
    const item = document.createElement("li");
    const hora = new Date(message.sentAt).toLocaleTimeString();

    item.textContent =
        `${message.user}: ${message.message} (${hora})`;

    messagesList.appendChild(item);
});

connection.onreconnecting(() => {
    status.textContent = "Reconectando...";
    sendButton.disabled = true;
    joinRoomButton.disabled = true;
    salaUnida = null;
});

connection.onreconnected(() => {
    status.textContent = "Conectado. Únete a una sala.";
    sendButton.disabled = true;
    joinRoomButton.disabled = false;
    salaUnida = null;
});

async function startConnection() {
    try {
        await connection.start();

        status.textContent = "Conectado. Únete a una sala.";
        sendButton.disabled = true;
        joinRoomButton.disabled = false;
    } catch (error) {
        status.textContent = "Error de conexión";
        console.error(error);
    }
}

joinRoomButton.addEventListener("click", async () => {
    const sala = roomInput.value;

    if (connection.state !== signalR.HubConnectionState.Connected) {
        alert("No existe una conexión activa.");
        return;
    }

    try {
        await connection.invoke("UnirseASala", sala);

        salaUnida = sala;
        status.textContent = `Conectado a la sala ${sala}`;
        sendButton.disabled = false;
    } catch (error) {
        console.error("No se pudo entrar a la sala:", error);
        status.textContent = "No se pudo entrar a la sala";
    }
});

sendButton.addEventListener("click", async () => {
    const user = document.getElementById("userInput").value.trim();
    const message = document.getElementById("messageInput").value.trim();

    if (!salaUnida) {
        alert("Primero debes unirte a una sala.");
        return;
    }

    if (!user || !message) {
        alert("Completa el usuario y el mensaje.");
        return;
    }

    if (connection.state !== signalR.HubConnectionState.Connected) {
        alert("No existe una conexión activa.");
        return;
    }

    try {
        await connection.invoke("SendMessage", salaUnida, user, message);
        document.getElementById("messageInput").value = "";
    } catch (error) {
        console.error("No se pudo enviar el mensaje:", error);
        status.textContent = "No se pudo enviar el mensaje";
    }
});

startConnection();

package com.sagrd.androidchat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.annotations.SerializedName
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import com.sagrd.androidchat.ChatViewModel.Companion.SALA_AUTOMATICA
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ChatViewModel : ViewModel() {

    companion object {
        const val SALA_AUTOMATICA = "Desarrollo"
    }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // 1. Configurar la conexión al Hub
    private val hubConnection: HubConnection = HubConnectionBuilder
        .create("https://chathub-b2hdatgbenfca9f5.eastus2-01.azurewebsites.net/chatHub")
        .build()

    init {
        // 2. Escuchar eventos del servidor
        hubConnection.on("ReceiveMessage", { mensaje: ChatMessage ->
            val currentUsername = _uiState.value.username.trim()
            val enrichedMessage = mensaje.copy(
                isSender = mensaje.user.isNotBlank() &&
                        mensaje.user.equals(currentUsername, ignoreCase = true)
            )
            val nuevaLista = _uiState.value.messages + enrichedMessage
            _uiState.update { it.copy(messages = nuevaLista) }
        }, ChatMessage::class.java)

        // 3. Manejar desconexión
        hubConnection.onClosed { error: Exception? ->
            _uiState.update {
                it.copy(
                    connectionStatus = ConnectionStatus.DISCONNECTED,
                    statusText = if (error != null) "Desconectado por error" else "Desconectado",
                    isConnecting = false,
                    errorMessage = error?.message
                )
            }
        }

        // 4. Iniciar la conexión y unirse automáticamente a "Desarrollo"
        conectarHub()
    }

    fun conectarHub() {
        _uiState.update {
            it.copy(
                isConnecting = true,
                connectionStatus = ConnectionStatus.CONNECTING,
                statusText = "Conectando...",
                errorMessage = null
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (hubConnection.connectionState != HubConnectionState.CONNECTED) {
                    hubConnection.start().blockingAwait()
                }

                // Unirse automáticamente a la sala "Desarrollo"
                hubConnection.invoke("UnirseASala", SALA_AUTOMATICA).blockingAwait()

                _uiState.update {
                    it.copy(
                        isConnecting = false,
                        connectionStatus = ConnectionStatus.CONNECTED,
                        statusText = "Conectado a la sala $SALA_AUTOMATICA",
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                val mensaje = "Error al conectar: ${e.message ?: "Sin conexión"}"
                Log.e("conectarHub", mensaje)
                _uiState.update {
                    it.copy(
                        isConnecting = false,
                        connectionStatus = ConnectionStatus.DISCONNECTED,
                        statusText = "Error de conexión",
                        errorMessage = mensaje
                    )
                }
            }
        }
    }

    fun onUsernameChanged(newUsername: String) {
        if (newUsername.length <= 40) {
            _uiState.update { currentState ->
                val trimmed = newUsername.trim()
                val updatedMessages = currentState.messages.map { msg ->
                    msg.copy(
                        isSender = trimmed.isNotBlank() &&
                                msg.user.equals(trimmed, ignoreCase = true)
                    )
                }
                currentState.copy(
                    username = newUsername,
                    messages = updatedMessages
                )
            }
        }
    }

    fun onMessageInputChanged(newMessage: String) {
        if (newMessage.length <= 1000) {
            _uiState.update { it.copy(currentMessage = newMessage) }
        }
    }

    fun enviarMensaje() {
        val state = _uiState.value
        val user = state.username.trim()
        val messageText = state.currentMessage.trim()

        if (user.isBlank() || messageText.isBlank()) {
            _uiState.update { it.copy(errorMessage = "El usuario y el mensaje son obligatorios.") }
            return
        }

        if (user.length > 40) {
            _uiState.update { it.copy(errorMessage = "El usuario no debe superar los 40 caracteres.") }
            return
        }

        if (messageText.length > 1000) {
            _uiState.update { it.copy(errorMessage = "El mensaje no debe superar los 1000 caracteres.") }
            return
        }

        if (hubConnection.connectionState != HubConnectionState.CONNECTED) {
            _uiState.update { it.copy(errorMessage = "No existe una conexión activa.") }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                hubConnection.invoke("SendMessage", SALA_AUTOMATICA, user, messageText).blockingAwait()
                _uiState.update { it.copy(currentMessage = "", errorMessage = null) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = "No se pudo enviar el mensaje: ${e.message ?: "Error desconocido"}"
                    )
                }
            }
        }
    }

    override fun onCleared() {
        try {
            hubConnection.stop()
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error al cerrar la conexión", e)
        }
    }
}

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

data class ChatMessage(
    @SerializedName(value = "user", alternate = ["User"])
    val user: String = "",

    @SerializedName(value = "message", alternate = ["Message"])
    val message: String = "",

    @SerializedName(value = "sentAt", alternate = ["SentAt"])
    val sentAt: String = "",

    val isSender: Boolean = false
) {

    val horaFormateada: String
        get() {
            if (sentAt.isBlank()) return ""
            return try {
                val instant = Instant.parse(sentAt)
                val localTime = instant.atZone(ZoneId.systemDefault()).toLocalTime()
                localTime.format(DateTimeFormatter.ofPattern("hh:mm a"))
            } catch (e: Exception) {
                if (sentAt.length >= 16 && sentAt.contains("T")) {
                    sentAt.substring(11, 16)
                } else {
                    sentAt
                }
            }
        }
}

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val connectionStatus: ConnectionStatus = ConnectionStatus.CONNECTING,
    val statusText: String = "Conectando...",
    val sala: String = SALA_AUTOMATICA,
    val username: String = "",
    val currentMessage: String = "",
    val errorMessage: String? = null,
    val isConnecting: Boolean = false
) {
    val isConnected: Boolean
        get() = connectionStatus == ConnectionStatus.CONNECTED

    val canSendMessage: Boolean
        get() = isConnected && !isConnecting
}
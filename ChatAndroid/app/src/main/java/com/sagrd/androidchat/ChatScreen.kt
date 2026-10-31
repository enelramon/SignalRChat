package com.sagrd.androidchat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sagrd.androidchat.ui.theme.AndroidChatTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    uiState: ChatUiState,
    onUsernameChanged: (String) -> Unit,
    onMessageChanged: (String) -> Unit,
    onSendMessage: () -> Unit,
    onReconnect: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        topBar = { TopBar(uiState, onReconnect) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) { 
            AnimatedVisibility(visible = uiState.errorMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text(
                        text = uiState.errorMessage ?: "",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            //Sala de chat
            OutlinedTextField(
                label = { Text("Sala de Chat") },
                value = uiState.sala,
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            // Campo de Nombre de Usuario
            OutlinedTextField(
                label = { Text("Usuario") },
                placeholder = { Text("Escribe tu nombre...") },
                value = uiState.username,
                onValueChange = onUsernameChanged,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            // Contenedor de Historial de Chats
            HistorialChats(uiState)

            Spacer(modifier = Modifier.height(8.dp))

            // Mensaje y Botón de Enviar
            EditorMensajes(uiState, onMessageChanged, onSendMessage)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    uiState: ChatUiState,
    onReconnect: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "Chat en tiempo real",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        actions = {
            if (uiState.isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 8.dp),
                    strokeWidth = 2.dp
                )
            } else if (!uiState.isConnected) {
                IconButton(onClick = onReconnect) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reconectar Hub"
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    )
}

@Composable
private fun ColumnScope.HistorialChats(uiState: ChatUiState) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        shadowElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface
    ) {
        if (uiState.messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay mensajes en la conversación. ¡Envía el primer mensaje!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val listState = rememberLazyListState()

            LaunchedEffect(uiState.messages.size) {
                if (uiState.messages.isNotEmpty()) {
                    listState.animateScrollToItem(uiState.messages.size - 1)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.messages) { msg ->
                    val isSender = msg.isSender || (
                            uiState.username.isNotBlank() &&
                                    msg.user.equals(
                                        uiState.username.trim(),
                                        ignoreCase = true
                                    )
                            )
                    Mensaje(
                        usuario = msg.user,
                        mensaje = msg.message,
                        hora = msg.horaFormateada,
                        isSender = isSender
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorMensajes(
    uiState: ChatUiState,
    onMessageChanged: (String) -> Unit,
    onSendMessage: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            label = { Text("Mensaje") },
            placeholder = { Text("Escribe un mensaje...") },
            value = uiState.currentMessage,
            onValueChange = onMessageChanged,
            modifier = Modifier.weight(1f),
            maxLines = 3,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = {
                if (uiState.canSendMessage && uiState.username.isNotBlank() && uiState.currentMessage.isNotBlank()) {
                    onSendMessage()
                }
            })
        )

        Button(
            onClick = onSendMessage,
            enabled = uiState.canSendMessage &&
                    uiState.username.isNotBlank() &&
                    uiState.currentMessage.isNotBlank(),
            modifier = Modifier.height(56.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Enviar mensaje"
            )
        }
    }
}

@Composable
fun Mensaje(
    usuario: String,
    mensaje: String,
    hora: String,
    isSender: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isSender) Arrangement.End else Arrangement.Start
    ) {
        ElevatedCard(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isSender) 16.dp else 2.dp,
                bottomEnd = if (isSender) 2.dp else 16.dp
            ),
            colors = CardDefaults.elevatedCardColors(
                containerColor = if (isSender)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = usuario,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSender)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.secondary
                    )
                    if (hora.isNotBlank()) {
                        Text(
                            text = hora,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = mensaje,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSender)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun ChatScreenPreview() {
    val sampleState = ChatUiState(
        messages = listOf(
            ChatMessage(
                user = "Alice",
                message = "¡Hola! Bienvenidos al chat de Desarrollo.",
                sentAt = "2026-10-27T10:00:00Z",
                isSender = false
            ),
            ChatMessage(
                user = "Bob",
                message = "Hola Alice, la conexión se realiza automáticamente a Desarrollo.",
                sentAt = "2026-10-27T10:01:00Z",
                isSender = true
            ),
            ChatMessage(
                user = "Alice",
                message = "¡Excelente! Ya no hace falta seleccionar sala.",
                sentAt = "2026-10-27T10:02:00Z",
                isSender = false
            )
        ),
        username = "Bob",
        connectionStatus = ConnectionStatus.CONNECTED,
        statusText = "Conectado a la sala Desarrollo"
    )

    AndroidChatTheme {
        ChatScreen(
            uiState = sampleState,
            onUsernameChanged = {},
            onMessageChanged = {},
            onSendMessage = {},
            onReconnect = {}
        )
    }
}

package com.kashif.myapplication


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.kashif.myapplication.communication.CommunicationManager
import com.kashif.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var communicationManager: CommunicationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        communicationManager = CommunicationManager(this)

        setContent {
            MyApplicationTheme {
                MainScreen(communicationManager)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        communicationManager.connect()
    }

    override fun onDestroy() {
        super.onDestroy()
        communicationManager.disconnect()
    }
}

@Composable
fun MainScreen(communicationManager: CommunicationManager) {
    val scope = rememberCoroutineScope()
    val messages = remember { mutableStateListOf<String>() }

    // Collect messages from the flow
    LaunchedEffect(Unit) {
        communicationManager.messageFlow.collect { message ->
            messages.add("Received: ${message.content}")
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        content = { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                Text("Messages Client \"com.kashif.myapplication\"")
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(messages) { message ->
                        Text(text = message)
                    }
                }
                Button(onClick = {
                    scope.launch {
                        communicationManager.sendMessage("Hello from Compose!")
                        messages.add("Sent: Hello from Compose!")
                    }
                }) {
                    Text("Send")
                }
            }
        }
    )
}


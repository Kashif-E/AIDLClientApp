// ClientApp/src/main/java/com/kashif/myapplication/communication/CommunicationManager.kt
package com.kashif.myapplication.communication

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.kashif.aidleventcommunicator.communication.ICommunicationService
import com.kashif.aidleventcommunicator.communication.IMessageListener
import com.kashif.aidleventcommunicator.message.IMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class CommunicationManager(private val context: Context) {

    private var communicationService: ICommunicationService? = null
    private var isBound = false

    private val _messageFlow = MutableSharedFlow<IMessage>()
    val messageFlow: SharedFlow<IMessage> = _messageFlow

    private val messageListener = object : IMessageListener.Stub() {
        override fun onMessageReceived(message: IMessage) {
            // Emit the received message to the flow
            CoroutineScope(Dispatchers.Main).launch {
                _messageFlow.emit(message)
                Log.d("CommunicationManager", "Message received: ${message.content}")
            }
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            communicationService = ICommunicationService.Stub.asInterface(service)
            isBound = true
            // Register the listener
            communicationService?.registerListener(messageListener)
            Log.d("CommunicationManager", "Service Connected")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            // Unregister the listener and clean up
            communicationService?.unregisterListener(messageListener)
            communicationService = null
            isBound = false
            Log.d("CommunicationManager", "Service Disconnected")
        }

        override fun onBindingDied(name: ComponentName?) {
            communicationService = null
            isBound = false
            Log.d("CommunicationManager", "Binding Died")
        }

        override fun onNullBinding(name: ComponentName?) {
            communicationService = null
            isBound = false
            Log.d("CommunicationManager", "Null Binding")
        }
    }

    fun connect() {
        if (!isBound) {
            val intent = Intent("com.kashif.aidleventcommunicator.communication.ICommunicationService").apply {
                setPackage("com.kashif.aidlmessage") // Must match the server's package name
            }
            val bindResult = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            Log.d("CommunicationManager", "Binding to service result: $bindResult")
        }
    }

    fun disconnect() {
        if (isBound) {
            communicationService?.unregisterListener(messageListener)
            context.unbindService(serviceConnection)
            isBound = false
            Log.d("CommunicationManager", "Unbinding from service")
        }
    }

    suspend fun sendMessage(content: String) {
        withContext(Dispatchers.IO) {
            communicationService?.sendMessage(IMessage(content))
            Log.d("CommunicationManager", "sendMessage called with: $content")
        }
    }
}
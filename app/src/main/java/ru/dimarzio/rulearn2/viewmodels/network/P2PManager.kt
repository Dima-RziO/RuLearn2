package ru.dimarzio.rulearn2.viewmodels.network

import android.net.Uri
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import ru.dimarzio.rulearn2.BuildConfig
import ru.dimarzio.rulearn2.application.Database
import ru.dimarzio.rulearn2.viewmodels.ErrorHandler

class P2PManager(
    private val client: ConnectionsClient,
    private val database: Database,
    private val handler: ErrorHandler
) {
    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(id: String, payload: Payload) {
            when (payload.type) {
                Payload.Type.BYTES -> {
                    if (String(payload.asBytes() ?: byteArrayOf()) == "PULL") {
                        client.sendPayload(id, Payload.fromFile(database.path))
                    }
                }

                Payload.Type.FILE -> {
                    val uri = payload.asFile()?.asUri()
                    if (uri != null) {
                        onFileReceived(id, uri)
                    } else {
                        handler.onMessageReceived("Error.")
                    }

                    client.disconnectFromEndpoint(id)
                }
            }
        }

        override fun onPayloadTransferUpdate(id: String, update: PayloadTransferUpdate) {
            when (update.status) {
                PayloadTransferUpdate.Status.IN_PROGRESS -> {
                    transferProgress = update.bytesTransferred.toFloat() / update.totalBytes * 100
                }

                PayloadTransferUpdate.Status.SUCCESS -> {
                    transferProgress = null
                }

                PayloadTransferUpdate.Status.FAILURE -> {
                    transferProgress = null
                    handler.onMessageReceived("Error.")
                }
            }
        }
    }

    private val connectionCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(id: String, info: ConnectionInfo) {
            connectingId = id
            connectionCode = info.authenticationDigits
        }

        override fun onConnectionResult(id: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                connectedId = id
            } else {
                result.status.connectionResult?.errorMessage?.let(handler::onMessageReceived)
            }

            connectingId = null
        }

        override fun onDisconnected(id: String) {
            if (connectedId == id) {
                connectedId = null
            }

            if (connectingId == id) {
                connectingId = null
            }
        }
    }

    lateinit var onFileReceived: (String, Uri) -> Unit // Id & Uri

    val endpoints = mutableStateMapOf<String, String>()

    var advertising by mutableStateOf(false)
        private set

    var connectedId by mutableStateOf<String?>(null)
        private set
    var connectingId by mutableStateOf<String?>(null)
        private set

    var connectionCode by mutableStateOf<String?>(null)
        private set

    var transferProgress by mutableStateOf<Float?>(null)
        private set

    fun startAdvertising() {
        client.startAdvertising(
            Build.MODEL,
            BuildConfig.APPLICATION_ID,
            connectionCallback,
            AdvertisingOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
        )

        advertising = true
    }

    fun stopAdvertising() {
        client.stopAdvertising()
        advertising = false
    }

    fun startDiscovery() {
        endpoints.clear()

        val callback = object : EndpointDiscoveryCallback() {
            override fun onEndpointFound(id: String, info: DiscoveredEndpointInfo) {
                endpoints[id] = info.endpointName
            }

            override fun onEndpointLost(id: String) {
                endpoints.remove(id)
            }
        }

        client.startDiscovery(
            BuildConfig.APPLICATION_ID,
            callback,
            DiscoveryOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
        )
    }

    fun stopDiscovery() {
        client.stopDiscovery()
    }

    fun connect(id: String) {
        client.requestConnection(Build.MODEL, id, connectionCallback)
    }

    fun acceptConnection() {
        connectedId?.let { client.disconnectFromEndpoint(it) }
        if (connectingId != null) {
            client.acceptConnection(connectingId!!, payloadCallback)
        }

        connectionCode = null
    }

    fun rejectConnection() {
        if (connectingId != null) {
            client.rejectConnection(connectingId!!)
        } else {
            // Already rejected by hub.
        }

        connectionCode = null
    }

    fun pull() {
        if (connectedId != null) {
            client.sendPayload(connectedId!!, Payload.fromBytes("PULL".toByteArray()))
        }
    }

    fun stop() {
        client.stopAllEndpoints()
    }
}
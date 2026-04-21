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
import ru.dimarzio.rulearn2.viewmodels.ErrorHandler
import java.io.File

class P2PManager(private val client: ConnectionsClient, private val handler: ErrorHandler) {
    private var connectedId: String? = null

    lateinit var onFileReceived: (Uri) -> Unit

    val endpoints = mutableStateMapOf<String, String>()

    var transferProgress by mutableStateOf(null as Float?)
        private set

    val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(id: String, payload: Payload) {
            if (payload.type == Payload.Type.FILE) {
                val uri = payload.asFile()?.asUri()
                if (uri != null) {
                    onFileReceived(uri)
                } else {
                    handler.onMessageReceived("Error.")
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

    val connectionCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(id: String, info: ConnectionInfo) {
            client.acceptConnection(id, payloadCallback)
        }

        override fun onConnectionResult(id: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                connectedId = id
            } else {
                result.status.connectionResult?.errorMessage?.let(handler::onMessageReceived)
            }
        }

        override fun onDisconnected(id: String) {
            connectedId = null
        }
    }

    fun startAdvertising() {
        client.startAdvertising(
            Build.MODEL,
            BuildConfig.APPLICATION_ID,
            connectionCallback,
            AdvertisingOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
        )
    }

    fun startDiscovery() {
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

    fun download(id: String, file: File) {
        client.requestConnection(Build.MODEL, id, connectionCallback)
    }
}
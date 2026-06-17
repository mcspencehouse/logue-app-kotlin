package com.spencehouse.logue.service.mqtt

import android.util.Log
import com.spencehouse.logue.service.Config
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.*

class AwsMqttClient(
    private val vin: String,
    private val cigToken: String,
    private val cigSignature: String,
    private val onMessageCallback: (String, String) -> Unit,
    private val onConnected: () -> Unit,
    private val onError: (String) -> Unit
) {
    private val tag = "AwsMqttClient"
    private var client: MqttAsyncClient? = null

    fun connect() {
        try {
            val clientId = "paho${System.currentTimeMillis()}"
            val serverUri = "wss://${Config.MQTT_HOST}:443/mqtt"
            
            Log.d(tag, "Connecting to $serverUri with clientId $clientId")
            client = MqttAsyncClient(serverUri, clientId, MemoryPersistence())
            
            val options = MqttConnectOptions()
            options.isCleanSession = true
            options.connectionTimeout = 60
            options.keepAliveInterval = 60
            
            val headers = Properties()
            headers.setProperty("X-Amz-CustomAuthorizer-Signature", cigSignature)
            headers.setProperty("prod_key", cigToken)
            headers.setProperty("X-Amz-CustomAuthorizer-Name", Config.MQTT_AUTHORIZER_NAME)
            headers.setProperty("User-Agent", "?SDK=Android&Version=2.75.0")
            
            // Set custom headers for AWS IoT WebSockets
            options.customWebSocketHeaders = headers
            
            val sslProps = Properties()
            sslProps.setProperty("com.ibm.ssl.protocol", "TLSv1.2")
            options.sslProperties = sslProps
            options.isHttpsHostnameVerificationEnabled = false

            client?.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    Log.e(tag, "Connection lost", cause)
                    onError("Connection lost: ${cause?.message}")
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    val payload = message?.let { String(it.payload) } ?: ""
                    Log.d(tag, "Message arrived on topic $topic: $payload")
                    onMessageCallback(topic ?: "", payload)
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {}
            })

            client?.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.i(tag, "Connected to AWS IoT")
                    
                    // Subscribe to shadow accepted topics
                    subscribe("\$aws/things/thing_$vin/shadow/name/DASHBOARD_ASYNC/update/accepted")
                    subscribe("\$aws/things/thing_$vin/shadow/name/ENGINE_START_STOP_ASYNC/update/accepted")
                    subscribe("\$aws/things/thing_$vin/shadow/name/CARFINDER_LOCATION_ASYNC/update/accepted")
                    subscribe("\$aws/things/thing_$vin/shadow/name/CARFINDER_HORN_LIGHT_ASYNC/update/accepted")
                    
                    onConnected()
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e(tag, "Connect failed", exception)
                    onError("Connect failed: ${exception?.message}")
                }
            })

        } catch (e: Exception) {
            Log.e(tag, "Connect error", e)
            onError(e.message ?: "Unknown error")
        }
    }

    fun disconnect() {
        try {
            if (client?.isConnected == true) {
                client?.disconnect()
            }
        } catch (e: Exception) {
            Log.e(tag, "Disconnect failed", e)
        }
        try {
            client?.close()
        } catch (e: Exception) {
            Log.e(tag, "Client close failed", e)
        }
    }

    fun subscribe(topic: String) {
        try {
            client?.subscribe(topic, 1, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(tag, "Successfully subscribed to $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e(tag, "Failed to subscribe to $topic", exception)
                }
            })
        } catch (e: Exception) {
            Log.e(tag, "Subscribe exception", e)
        }
    }
}

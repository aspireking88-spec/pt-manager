package com.example.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.UUID

sealed class PrinterConnectionState {
    object Disconnected : PrinterConnectionState()
    object Connecting : PrinterConnectionState()
    data class Connected(val deviceName: String, val address: String) : PrinterConnectionState()
    data class Error(val message: String) : PrinterConnectionState()
}

class BluetoothPrinterManager(private val context: Context) {

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    private val _connectionState = MutableStateFlow<PrinterConnectionState>(PrinterConnectionState.Disconnected)
    val connectionState: StateFlow<PrinterConnectionState> = _connectionState.asStateFlow()

    private val _selectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    val selectedDevice: StateFlow<BluetoothDevice?> = _selectedDevice.asStateFlow()

    val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        return try {
            bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun selectDevice(device: BluetoothDevice?) {
        _selectedDevice.value = device
    }

    @SuppressLint("MissingPermission")
    suspend fun connect(device: BluetoothDevice): Boolean = withContext(Dispatchers.IO) {
        _connectionState.value = PrinterConnectionState.Connecting
        try {
            disconnectInternal()
            val sppUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
            val s = device.createRfcommSocketToServiceRecord(sppUuid)
            s.connect()
            socket = s
            outputStream = s.outputStream
            _connectionState.value = PrinterConnectionState.Connected(
                deviceName = device.name ?: "Thermal Printer",
                address = device.address
            )
            true
        } catch (e: Exception) {
            disconnectInternal()
            _connectionState.value = PrinterConnectionState.Error("Connection failed: ${e.localizedMessage ?: "Unknown error"}")
            false
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        disconnectInternal()
        _connectionState.value = PrinterConnectionState.Disconnected
    }

    private fun disconnectInternal() {
        try {
            outputStream?.close()
        } catch (_: Exception) {}
        try {
            socket?.close()
        } catch (_: Exception) {}
        outputStream = null
        socket = null
    }

    suspend fun print(data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        val stream = outputStream
        if (stream == null || socket?.isConnected != true) {
            _connectionState.value = PrinterConnectionState.Error("Printer is not connected")
            return@withContext false
        }
        try {
            stream.write(data)
            stream.flush()
            true
        } catch (e: Exception) {
            disconnectInternal()
            _connectionState.value = PrinterConnectionState.Error("Print failed: ${e.localizedMessage}")
            false
        }
    }

    suspend fun printTestReceipt(settings: PrintDesignSettings): Boolean {
        val printer = EscPosPrinter()
            .reset()
            .alignCenter()
            .bold(true)
            .textSize(2, 2)
            .line("TEST RECEIPT")
            .textSize(1, 1)
            .bold(false)
            .line("Bluetooth Thermal Printer OK")
            .divider('=', 32)
            .alignLeft()
            .line("Printer: Connected")
            .line("Time   : ${java.util.Date()}")
            .divider('-', 32)
            .alignCenter()
            .line("Printer V1 Operational")
            .feed(3)
            .cut()

        return print(printer.build())
    }
}

package com.example

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.db.VoucherHistoryRepository
import com.example.printer.BluetoothPrinterManager
import com.example.printer.PrinterConnectionState
import com.example.printer.PrinterSocket
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PrinterDisconnectDetectionTest {

    private lateinit var context: Context
    private lateinit var historyRepository: VoucherHistoryRepository
    private lateinit var printerManager: BluetoothPrinterManager
    private lateinit var testDevice: BluetoothDevice

    class FakeInputStream : InputStream() {
        @Volatile var shouldThrow: Boolean = false
        @Volatile var eof: Boolean = false
        @Volatile var isClosed: Boolean = false

        override fun read(): Int {
            while (!shouldThrow && !eof && !isClosed) {
                try {
                    Thread.sleep(10)
                } catch (_: InterruptedException) {
                    return -1
                }
            }
            if (isClosed || shouldThrow) throw IOException("Simulated read exception")
            if (eof) return -1
            return 0
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            while (!shouldThrow && !eof && !isClosed) {
                try {
                    Thread.sleep(10)
                } catch (_: InterruptedException) {
                    return -1
                }
            }
            if (isClosed || shouldThrow) throw IOException("Simulated read exception")
            if (eof) return -1
            return 1
        }

        override fun close() {
            isClosed = true
        }
    }

    class FakeOutputStream : OutputStream() {
        @Volatile var shouldThrow: Boolean = false
        @Volatile var isClosed: Boolean = false

        override fun write(b: Int) {
            if (shouldThrow || isClosed) throw IOException("Simulated write error: Broken pipe")
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            if (shouldThrow || isClosed) throw IOException("Simulated write error: Broken pipe")
        }

        override fun flush() {
            if (shouldThrow || isClosed) throw IOException("Simulated flush error: Connection reset")
        }

        override fun close() {
            isClosed = true
        }
    }

    class FakePrinterSocket(
        var connected: Boolean = true
    ) : PrinterSocket {
        var isClosed: Boolean = false
        override val isConnected: Boolean get() = connected && !isClosed

        val fakeIn = FakeInputStream()
        val fakeOut = FakeOutputStream()

        override val inputStream: InputStream get() = fakeIn
        override val outputStream: OutputStream get() = fakeOut

        override fun close() {
            isClosed = true
            connected = false
            fakeIn.close()
            fakeOut.close()
        }
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val dao = AppDatabase.getDatabase(context).voucherHistoryDao()
        historyRepository = VoucherHistoryRepository(dao)

        printerManager = BluetoothPrinterManager(context, historyRepository)
        printerManager.isBluetoothEnabledOverride = { true }
        printerManager.healthCheckIntervalMs = 25L // Fast health-check interval for testing

        testDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice("66:22:9D:D7:DE:13")
        printerManager.remoteDeviceResolver = { testDevice }
        printerManager.socketFactory = { throw IOException("Offline by default in tests") }
    }

    @After
    fun tearDown() {
        printerManager.cleanup()
    }

    @Test
    fun test1_connected_to_bluetooth_off_transitions_to_disconnected() = runTest {
        val fakeSocket = FakePrinterSocket()
        printerManager.socketFactory = { fakeSocket }

        val connected = printerManager.connect(testDevice)
        assertTrue("Initial connection should succeed", connected)
        assertTrue("State must be Connected", printerManager.connectionState.value is PrinterConnectionState.Connected)

        // Simulate Bluetooth turned OFF broadcast from OS
        val intent = Intent(BluetoothAdapter.ACTION_STATE_CHANGED).apply {
            putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)
        }
        printerManager.connectionEventReceiver.onReceive(context, intent)

        assertEquals(
            "State must transition to Disconnected when Bluetooth is turned off",
            PrinterConnectionState.Disconnected,
            printerManager.connectionState.value
        )
    }

    @Test
    fun test2_connected_to_socket_ioexception_transitions_to_disconnected() = runTest {
        val fakeSocket = FakePrinterSocket()
        printerManager.socketFactory = { fakeSocket }

        val connected = printerManager.connect(testDevice)
        assertTrue(connected)
        assertTrue(printerManager.connectionState.value is PrinterConnectionState.Connected)

        // Cause IOException during background socket reading
        fakeSocket.fakeIn.shouldThrow = true

        var attempts = 0
        while (printerManager.connectionState.value is PrinterConnectionState.Connected && attempts < 50) {
            Thread.sleep(15)
            attempts++
        }

        assertEquals(
            "State must transition to Disconnected upon socket IOException",
            PrinterConnectionState.Disconnected,
            printerManager.connectionState.value
        )
    }

    @Test
    fun test3_connected_to_socket_closed_transitions_to_disconnected() = runTest {
        val fakeSocket = FakePrinterSocket()
        printerManager.socketFactory = { fakeSocket }

        val connected = printerManager.connect(testDevice)
        assertTrue(connected)
        assertTrue(printerManager.connectionState.value is PrinterConnectionState.Connected)

        // Close socket locally or remotely
        fakeSocket.close()

        var attempts = 0
        while (printerManager.connectionState.value is PrinterConnectionState.Connected && attempts < 50) {
            Thread.sleep(15)
            attempts++
        }

        assertEquals(
            "State must transition to Disconnected when socket is closed",
            PrinterConnectionState.Disconnected,
            printerManager.connectionState.value
        )
    }

    @Test
    fun test4_connected_to_unreachable_printer_detected_by_health_check() = runTest {
        val fakeSocket = FakePrinterSocket()
        printerManager.socketFactory = { fakeSocket }

        val connected = printerManager.connect(testDevice)
        assertTrue(connected)
        assertTrue(printerManager.connectionState.value is PrinterConnectionState.Connected)

        // Printer link dropped / out of range: health-check ping write/flush fails
        fakeSocket.fakeOut.shouldThrow = true

        var attempts = 0
        while (printerManager.connectionState.value is PrinterConnectionState.Connected && attempts < 50) {
            Thread.sleep(15)
            attempts++
        }

        assertEquals(
            "Health check must transition state to Disconnected when printer link drops",
            PrinterConnectionState.Disconnected,
            printerManager.connectionState.value
        )
    }

    @Test
    fun test5_disconnected_to_successful_reconnect_becomes_connected() = runTest {
        val fakeSocket1 = FakePrinterSocket()
        printerManager.socketFactory = { fakeSocket1 }

        printerManager.connect(testDevice)
        assertTrue(printerManager.connectionState.value is PrinterConnectionState.Connected)

        // Disconnect
        printerManager.disconnect()
        assertEquals(PrinterConnectionState.Disconnected, printerManager.connectionState.value)

        // Reconnect with new socket
        val fakeSocket2 = FakePrinterSocket()
        printerManager.socketFactory = { fakeSocket2 }

        val reconnected = printerManager.connect(testDevice)
        assertTrue("Reconnection should succeed", reconnected)
        assertTrue(
            "State must be Connected after successful reconnection",
            printerManager.connectionState.value is PrinterConnectionState.Connected
        )
        val connectedState = printerManager.connectionState.value as PrinterConnectionState.Connected
        assertEquals("66:22:9D:D7:DE:13", connectedState.deviceAddress)
    }

    @Test
    fun test6_failed_connection_remains_not_connected() = runTest {
        printerManager.socketFactory = {
            throw IOException("Device not reachable / Connection refused")
        }

        val success = printerManager.connect(testDevice)
        assertFalse("Connection must report failure", success)
        assertFalse(
            "State must never be Connected upon failure",
            printerManager.connectionState.value is PrinterConnectionState.Connected
        )
        assertTrue(
            "State should be Error",
            printerManager.connectionState.value is PrinterConnectionState.Error
        )
    }

    @Test
    fun test7_bluetooth_disabled_before_connecting_fails_immediately() = runTest {
        printerManager.isBluetoothEnabledOverride = { false }

        val success = printerManager.connect(testDevice)
        assertFalse("Connection should fail when Bluetooth is disabled", success)
        assertFalse(
            "State must not be Connected",
            printerManager.connectionState.value is PrinterConnectionState.Connected
        )
    }

    @Test
    fun test8_remote_peer_eof_transitions_to_disconnected() = runTest {
        val fakeSocket = FakePrinterSocket()
        printerManager.socketFactory = { fakeSocket }

        printerManager.connect(testDevice)
        assertTrue(printerManager.connectionState.value is PrinterConnectionState.Connected)

        // Remote printer disconnects (read returns -1)
        fakeSocket.fakeIn.eof = true

        var attempts = 0
        while (printerManager.connectionState.value is PrinterConnectionState.Connected && attempts < 50) {
            Thread.sleep(15)
            attempts++
        }

        assertEquals(
            "State must transition to Disconnected when remote peer returns EOF",
            PrinterConnectionState.Disconnected,
            printerManager.connectionState.value
        )
    }

    @Test
    fun test9_acl_disconnected_broadcast_transitions_to_disconnected() = runTest {
        val fakeSocket = FakePrinterSocket()
        printerManager.socketFactory = { fakeSocket }

        printerManager.connect(testDevice)
        assertTrue(printerManager.connectionState.value is PrinterConnectionState.Connected)

        // System sends ACL disconnected broadcast for this device
        val intent = Intent(BluetoothDevice.ACTION_ACL_DISCONNECTED).apply {
            putExtra(BluetoothDevice.EXTRA_DEVICE, testDevice)
        }
        printerManager.connectionEventReceiver.onReceive(context, intent)

        assertEquals(
            "State must transition to Disconnected upon ACL_DISCONNECTED broadcast",
            PrinterConnectionState.Disconnected,
            printerManager.connectionState.value
        )
    }

    @Test
    fun test10_initial_default_printer_is_MPT_II() {
        assertEquals("MPT-II", printerManager.getDefaultPrinterName())
        assertEquals("66:22:9D:D7:DE:13", printerManager.getDefaultPrinterAddress())
        assertEquals("MPT-II", printerManager.defaultPrinterName.value)
        assertEquals("66:22:9D:D7:DE:13", printerManager.defaultPrinterAddress.value)
    }

    @Test
    fun test11_set_default_printer_persists_and_updates() {
        printerManager.setDefaultPrinter("Office Printer", "11:22:33:44:55:66")
        assertEquals("Office Printer", printerManager.getDefaultPrinterName())
        assertEquals("11:22:33:44:55:66", printerManager.getDefaultPrinterAddress())
        assertEquals("Office Printer", printerManager.defaultPrinterName.value)
        assertEquals("11:22:33:44:55:66", printerManager.defaultPrinterAddress.value)

        // Reset back to MPT-II default
        printerManager.setDefaultPrinter(BluetoothPrinterManager.DEFAULT_PRINTER_NAME, BluetoothPrinterManager.DEFAULT_PRINTER_ADDRESS)
    }

    @Test
    fun test12_autoConnectDefaultPrinter_connects_to_saved_default() = runTest {
        val fakeSocket = FakePrinterSocket()
        printerManager.socketFactory = { fakeSocket }

        val connected = printerManager.autoConnectDefaultPrinter()
        assertTrue("Auto-connect to default printer should succeed", connected)
        assertTrue("State must be Connected", printerManager.connectionState.value is PrinterConnectionState.Connected)
        val state = printerManager.connectionState.value as PrinterConnectionState.Connected
        assertEquals("66:22:9D:D7:DE:13", state.deviceAddress)
    }

    @Test
    fun test13_autoConnectDefaultPrinter_offline_fails_silently_without_error() = runTest {
        printerManager.socketFactory = {
            throw IOException("Device unreachable / powered off")
        }
        printerManager.disconnect()

        val connected = printerManager.autoConnectDefaultPrinter()
        assertFalse("Auto-connect to offline printer should return false", connected)
        assertEquals(
            "Auto-connect failure should silently leave state as Disconnected (no error spam)",
            PrinterConnectionState.Disconnected,
            printerManager.connectionState.value
        )
    }

    @Test
    fun test14_daemon_auto_connects_when_printer_powers_on_later() = runTest {
        var isPrinterPoweredOn = false
        printerManager.socketFactory = {
            if (!isPrinterPoweredOn) {
                throw IOException("Printer is powered OFF")
            } else {
                FakePrinterSocket()
            }
        }
        printerManager.disconnect()
        printerManager.autoConnectIntervalMs = 20L
        printerManager.startAutoConnectDaemon()

        // Initially printer is OFF
        assertEquals(PrinterConnectionState.Disconnected, printerManager.connectionState.value)

        // Wait a few daemon ticks while printer is OFF
        Thread.sleep(60)
        assertEquals("Printer remains disconnected while OFF", PrinterConnectionState.Disconnected, printerManager.connectionState.value)

        // User turns ON the default printer
        isPrinterPoweredOn = true

        // Daemon should automatically connect without user interaction
        var attempts = 0
        while (printerManager.connectionState.value !is PrinterConnectionState.Connected && attempts < 100) {
            Thread.sleep(20)
            attempts++
        }

        assertTrue("Daemon must automatically connect when printer is powered on", printerManager.connectionState.value is PrinterConnectionState.Connected)
        val connectedState = printerManager.connectionState.value as PrinterConnectionState.Connected
        assertEquals("66:22:9D:D7:DE:13", connectedState.deviceAddress)
    }

    @Test
    fun test15_daemon_auto_reconnects_after_temporary_disconnect() = runTest {
        printerManager.autoConnectIntervalMs = 20L
        val fakeSocket1 = FakePrinterSocket()
        var currentSocket: FakePrinterSocket = fakeSocket1
        var isPrinterAvailable = true

        printerManager.socketFactory = {
            if (!isPrinterAvailable) {
                throw IOException("Printer temporarily offline")
            } else {
                currentSocket
            }
        }
        printerManager.startAutoConnectDaemon()

        // Initial connect
        printerManager.connect(testDevice)
        assertTrue(printerManager.connectionState.value is PrinterConnectionState.Connected)

        // Printer disconnects temporarily (e.g. turned OFF / out of range)
        isPrinterAvailable = false
        fakeSocket1.close()

        var attempts = 0
        while (printerManager.connectionState.value is PrinterConnectionState.Connected && attempts < 100) {
            Thread.sleep(15)
            attempts++
        }
        assertEquals("State must be Disconnected", PrinterConnectionState.Disconnected, printerManager.connectionState.value)

        // Printer becomes available again (turned back ON)
        val fakeSocket2 = FakePrinterSocket()
        currentSocket = fakeSocket2
        isPrinterAvailable = true

        // Daemon should automatically reconnect
        attempts = 0
        while (printerManager.connectionState.value !is PrinterConnectionState.Connected && attempts < 100) {
            Thread.sleep(20)
            attempts++
        }

        assertTrue("Daemon must automatically reconnect to default printer", printerManager.connectionState.value is PrinterConnectionState.Connected)
    }

    @Test
    fun test16_auto_connect_triggered_by_bluetooth_state_on() = runTest {
        printerManager.isBluetoothEnabledOverride = { false }
        printerManager.disconnect()
        val fakeSocket = FakePrinterSocket()
        printerManager.socketFactory = { fakeSocket }

        // Start in disconnected state with Bluetooth OFF
        assertEquals(PrinterConnectionState.Disconnected, printerManager.connectionState.value)

        // Broadcast Bluetooth STATE_ON and enable Bluetooth
        printerManager.isBluetoothEnabledOverride = { true }
        val intent = Intent(BluetoothAdapter.ACTION_STATE_CHANGED).apply {
            putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_ON)
        }
        printerManager.connectionEventReceiver.onReceive(context, intent)

        var attempts = 0
        while (printerManager.connectionState.value !is PrinterConnectionState.Connected && attempts < 100) {
            Thread.sleep(20)
            attempts++
        }

        assertTrue("Bluetooth STATE_ON broadcast must trigger auto-connection", printerManager.connectionState.value is PrinterConnectionState.Connected)
    }

    @Test
    fun test17_auto_connect_triggered_by_action_found_default_printer() = runTest {
        printerManager.disconnect()
        val fakeSocket = FakePrinterSocket()
        printerManager.socketFactory = { fakeSocket }

        // Broadcast ACTION_FOUND with default printer device
        val intent = Intent(BluetoothDevice.ACTION_FOUND).apply {
            putExtra(BluetoothDevice.EXTRA_DEVICE, testDevice)
        }
        printerManager.connectionEventReceiver.onReceive(context, intent)

        var attempts = 0
        while (printerManager.connectionState.value !is PrinterConnectionState.Connected && attempts < 100) {
            Thread.sleep(20)
            attempts++
        }

        assertTrue("ACTION_FOUND matching default printer must trigger auto-connection", printerManager.connectionState.value is PrinterConnectionState.Connected)
    }
}

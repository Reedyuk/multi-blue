package dev.bluefalcon

import android.Manifest
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager

actual class BlueFalcon(private val context: Context) {

    init {
        if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            throw PermissionException()
    }

    actual val delegates: MutableList<BlueFalconDelegate> = arrayListOf()
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val mBluetoothScanCallBack = BluetoothScanCallBack()
    private val mGattClientCallback = GattClientCallback()

    actual fun connect(bluetoothPeripheral: BluetoothPeripheral) {
        log("connect")
        bluetoothPeripheral.bluetoothDevice.connectGatt(context, false, mGattClientCallback)
    }

    actual fun disconnect(bluetoothPeripheral: BluetoothPeripheral) {
        log("disconnect")
        mGattClientCallback.gatt?.disconnect()
        delegates.forEach {
            it.didDisconnect(bluetoothPeripheral)
        }
    }

    actual fun scan() {
        log("BT Scan started")
        val filter = ScanFilter.Builder().build()
        val filters = listOf(filter)
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()
        val bluetoothScanner = bluetoothManager.adapter?.bluetoothLeScanner
        bluetoothScanner?.startScan(filters, settings, mBluetoothScanCallBack)
    }

    actual fun readCharacteristic(
        bluetoothPeripheral: BluetoothPeripheral,
        bluetoothCharacteristic: BluetoothCharacteristic
    ) {
        mGattClientCallback.gatt?.readCharacteristic(bluetoothCharacteristic)
    }

    actual fun writeCharacteristic(
        bluetoothPeripheral: BluetoothPeripheral,
        bluetoothCharacteristic: BluetoothCharacteristic,
        value: String
    ) {
        bluetoothCharacteristic.setValue(value)
        mGattClientCallback.gatt?.writeCharacteristic(bluetoothCharacteristic)
    }

    inner class BluetoothScanCallBack: ScanCallback() {

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            addScanResult(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.forEach { addScanResult(it) }
        }

        override fun onScanFailed(errorCode: Int) {
            log("Failed to scan with code $errorCode")
        }

        private fun addScanResult(result: ScanResult?) {
            log("Found device ${result?.device?.address} name? ${result?.device?.name}")
            result?.let { scanResult ->
                scanResult.device?.let { device ->
                    delegates.forEach {
                        it.didDiscoverDevice(BluetoothPeripheral(device))
                    }
                }
            }
        }

    }

    inner class GattClientCallback: BluetoothGattCallback() {

        var gatt: BluetoothGatt? = null

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            log("onConnectionStateChange")
            this.gatt = gatt
            gatt?.let { bluetoothGatt ->
                bluetoothGatt.device.let {
                    bluetoothGatt.discoverServices()
                    delegates.forEach {
                        it.didConnect(BluetoothPeripheral(bluetoothGatt.device))
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            log("onServicesDiscovered")
            if (status != BluetoothGatt.GATT_SUCCESS) {
                return
            }
            gatt?.device?.let { bluetoothDevice ->
                gatt.services.let { services ->
                    val bluetoothPeripheral = BluetoothPeripheral(bluetoothDevice)
                    bluetoothPeripheral.services = services.toList()
                    delegates.forEach {
                        it.didDiscoverServices(bluetoothPeripheral)
                        it.didDiscoverCharacteristics(bluetoothPeripheral)
                    }
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            log("onMtuChanged$mtu status:$status")
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            log("onCharacteristicChanged")
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            log("onCharacteristicChanged")
        }

        private fun handleCharacteristicValueChange(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            characteristic?.let { forcedCharacteristic ->
                gatt?.device?.let { bluetoothDevice ->
                    delegates.forEach {
                        it.didCharacteristcValueChanged(BluetoothPeripheral(bluetoothDevice), forcedCharacteristic)
                    }
                }
            }
        }
    }

}
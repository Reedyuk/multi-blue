package dev.bluefalcon

actual class BlueFalcon {

    actual val delegates: MutableList<BlueFalconDelegate> = arrayListOf()

    actual fun connect(bluetoothPeripheral: BluetoothPeripheral) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    actual fun disconnect(bluetoothPeripheral: BluetoothPeripheral) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    actual fun scan() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    actual fun readCharacteristic(
        bluetoothPeripheral: BluetoothPeripheral,
        bluetoothCharacteristic: BluetoothCharacteristic
    ) {
        TODO("not implemented")
    }

    actual fun writeCharacteristic(
        bluetoothPeripheral: BluetoothPeripheral,
        bluetoothCharacteristic: BluetoothCharacteristic,
        value: String
    ) {
        TODO("not implemented")
    }

}
package io.github.pixelwatchspoof.hooks

import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanRecord
import android.util.Log
import android.util.SparseArray
import io.github.libxposed.api.XposedModule
import io.github.pixelwatchspoof.config.DeviceConfig

object DataInjectHook {

    private const val TAG = "DataInjectHook"
    private const val GOOGLE_COMPANY_ID = 224
    private const val XIAOMI_COMPANY_ID = 911

    fun hook(module: XposedModule, packageName: String, classLoader: ClassLoader) {
        when (packageName) {
            "com.mi.health" -> hookMiHealth(module, classLoader)
            "com.android.bluetooth" -> hookBluetoothScan(module, classLoader)
        }
    }

    private fun hookMiHealth(module: XposedModule, classLoader: ClassLoader) {
        module.log(Log.INFO, TAG, "Hooking com.mi.health for data injection")

        hookScanRecord(module, classLoader)
        hookAdvPacket(module, classLoader)
        hookBleDevice(module, classLoader)
    }

    private fun hookScanRecord(module: XposedModule, classLoader: ClassLoader) {
        try {
            val scanRecordClass = Class.forName("android.bluetooth.le.ScanRecord", false, classLoader)
            val fromBytesMethod = scanRecordClass.getDeclaredMethod("fromBytes", ByteArray::class.java)

            module.hook(fromBytesMethod).intercept { chain ->
                val rawBytes = chain.args[0] as? ByteArray
                val result = chain.proceed() as? ScanRecord

                if (result != null && rawBytes != null) {
                    val manufacturerData = result.manufacturerSpecificData
                    val hasXiaomiData = manufacturerData.get(XIAOMI_COMPANY_ID) != null

                    if (!hasXiaomiData) {
                        val modifiedBytes = injectXiaomiData(rawBytes)
                        if (modifiedBytes != null) {
                            val modifiedResult = fromBytesMethod.invoke(null, modifiedBytes) as? ScanRecord
                            if (modifiedResult != null) {
                                module.log(Log.DEBUG, TAG, "Injected Xiaomi data into ScanRecord")
                                return@intercept modifiedResult
                            }
                        }
                    }
                }
                result
            }
            module.log(Log.INFO, TAG, "hookScanRecord installed")
        } catch (e: Throwable) {
            module.log(Log.WARN, TAG, "hookScanRecord failed: ${e.message}")
        }
    }

    private fun hookAdvPacket(module: XposedModule, classLoader: ClassLoader) {
        try {
            val advPacketClass = Class.forName("com.xiaomi.fitness.mijia.sdk.data.AdvPacket", false, classLoader)
            module.log(Log.INFO, TAG, "Found AdvPacket class")

            for (method in advPacketClass.declaredMethods) {
                if (method.name.contains("parse") || method.name.contains("Parse") ||
                    method.name.contains("from") || method.name.contains("decode")) {
                    module.log(Log.DEBUG, TAG, "AdvPacket parse method: ${method.name}")
                }
            }
        } catch (e: Throwable) {
            module.log(Log.WARN, TAG, "AdvPacket not found: ${e.message}")
        }
    }

    private fun hookBleDevice(module: XposedModule, classLoader: ClassLoader) {
        try {
            val bleDeviceClass = Class.forName("com.xiaomi.fitness.device.manager.export.scan.BleDevice", false, classLoader)
            module.log(Log.INFO, TAG, "Found BleDevice class")

            for (method in bleDeviceClass.declaredMethods) {
                if (method.name.contains("get") || method.name.contains("set")) {
                    module.log(Log.DEBUG, TAG, "BleDevice method: ${method.name}")
                }
            }
        } catch (e: Throwable) {
            module.log(Log.WARN, TAG, "BleDevice not found: ${e.message}")
        }
    }

    private fun hookBluetoothScan(module: XposedModule, classLoader: ClassLoader) {
        module.log(Log.INFO, TAG, "Hooking com.android.bluetooth for scan data injection")

        try {
            val scannerClass = Class.forName("android.bluetooth.le.BluetoothLeScanner", false, classLoader)

            for (method in scannerClass.declaredMethods) {
                when (method.name) {
                    "startScan" -> {
                        module.hook(method).intercept { chain ->
                            module.log(Log.INFO, TAG, "BluetoothLeScanner.startScan called")
                            chain.proceed()
                        }
                        module.log(Log.INFO, TAG, "Hooked startScan")
                    }
                    "stopScan" -> {
                        module.hook(method).intercept { chain ->
                            module.log(Log.INFO, TAG, "BluetoothLeScanner.stopScan called")
                            chain.proceed()
                        }
                        module.log(Log.INFO, TAG, "Hooked stopScan")
                    }
                }
            }
        } catch (e: Throwable) {
            module.log(Log.WARN, TAG, "hookBluetoothScan failed: ${e.message}")
        }
    }

    private fun injectXiaomiData(rawBytes: ByteArray): ByteArray? {
        try {
            val xiaomiData = byteArrayOf(
                0x20, 0x01,
                0x3E, 0x00,
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00
            )

            val adType = 0xFF.toByte()
            val totalLen = (2 + xiaomiData.size).toByte()

            val newPacket = ByteArray(rawBytes.size + 2 + xiaomiData.size)
            System.arraycopy(rawBytes, 0, newPacket, 0, rawBytes.size)

            var pos = rawBytes.size
            newPacket[pos++] = totalLen
            newPacket[pos++] = adType
            System.arraycopy(xiaomiData, 0, newPacket, pos, xiaomiData.size)

            return newPacket
        } catch (e: Throwable) {
            return null
        }
    }
}

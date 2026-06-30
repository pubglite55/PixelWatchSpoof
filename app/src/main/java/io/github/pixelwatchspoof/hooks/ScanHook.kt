package io.github.pixelwatchspoof.hooks

import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.pixelwatchspoof.config.DeviceConfig

object ScanHook {

    private const val TAG = "ScanHook"

    fun hook(module: XposedModule, classLoader: ClassLoader) {
        hookBeaconRecognizer(module, classLoader)
    }

    private fun hookBeaconRecognizer(module: XposedModule, classLoader: ClassLoader) {
        try {
            val clazz = Class.forName(
                "com.xiaomi.fitness.device.manager.ui.scan.recognize.BeaconRecognizer",
                false,
                classLoader
            )
            module.log(Log.INFO, TAG, "Found BeaconRecognizer")

            val recognizeMethod = clazz.declaredMethods.firstOrNull {
                it.name == "recognize" && it.parameterTypes.size == 1
            }
            if (recognizeMethod == null) {
                module.log(Log.WARN, TAG, "recognize method not found")
                return
            }

            module.hook(recognizeMethod).intercept { chain ->
                val scanResult = chain.args[0]

                var originalResult: Any? = null
                try {
                    originalResult = chain.proceed()
                } catch (e: Throwable) {
                    module.log(Log.WARN, TAG, "recognize() threw: ${e.message}")
                }

                if (originalResult != null) {
                    return@intercept originalResult
                }

                val address = extractAddress(scanResult)
                val name = extractName(scanResult)
                val rssi = extractRssi(scanResult)

                if (address == null || !address.contains(DeviceConfig.PIXEL_WATCH_MAC_SUFFIX)) {
                    return@intercept null
                }

                module.log(Log.INFO, TAG, "Pixel Watch detected: $address ($name) rssi=$rssi")

                try {
                    val bleDeviceClass = Class.forName(
                        "com.xiaomi.fitness.device.manager.export.scan.BleDevice",
                        false, classLoader
                    )
                    val advPacketClass = Class.forName(
                        "com.xiaomi.fitness.mijia.sdk.data.AdvPacket",
                        false, classLoader
                    )

                    val advPacket = createAdvPacket(module, advPacketClass, address)

                    val constructor = bleDeviceClass.declaredConstructors.firstOrNull {
                        it.parameterTypes.size >= 8
                    }

                    if (constructor != null) {
                        val bleDevice = constructor.newInstance(
                            address,
                            address,
                            20863,
                            DeviceConfig.XiaomiWatch5.MODEL,
                            advPacket,
                            DeviceConfig.XiaomiWatch5.DEVICE_NAME,
                            rssi ?: -60,
                            false
                        )
                        module.log(Log.INFO, TAG, "Created fake BleDevice for Pixel Watch: $address")
                        return@intercept bleDevice
                    }
                } catch (e: Throwable) {
                    module.log(Log.WARN, TAG, "Failed to create BleDevice: ${e.message}")
                }

                null
            }

            module.log(Log.INFO, TAG, "Hooked BeaconRecognizer.recognize()")
        } catch (e: Throwable) {
            module.log(Log.WARN, TAG, "Failed to hook BeaconRecognizer: ${e.message}")
        }
    }

    private fun createAdvPacket(module: XposedModule, advPacketClass: Class<*>, mac: String): Any? {
        return try {
            val constructor = advPacketClass.declaredConstructors.firstOrNull()
            if (constructor != null) {
                val params = Array(constructor.parameterTypes.size) { i ->
                    when {
                        constructor.parameterTypes[i] == String::class.java -> mac
                        constructor.parameterTypes[i] == Int::class.javaPrimitiveType -> 0
                        constructor.parameterTypes[i] == Boolean::class.javaPrimitiveType -> false
                        constructor.parameterTypes[i] == Long::class.javaPrimitiveType -> 0L
                        constructor.parameterTypes[i] == ByteArray::class.java -> ByteArray(0)
                        else -> null
                    }
                }
                val instance = constructor.newInstance(*params)

                try {
                    val productIdField = advPacketClass.getDeclaredField("productId")
                    productIdField.isAccessible = true
                    productIdField.setInt(instance, 20863)
                } catch (_: Throwable) {}

                try {
                    val macField = advPacketClass.getDeclaredField("mac")
                    macField.isAccessible = true
                    macField.set(instance, mac)
                } catch (_: Throwable) {}

                instance
            } else {
                null
            }
        } catch (e: Throwable) {
            module.log(Log.WARN, TAG, "Failed to create AdvPacket: ${e.message}")
            null
        }
    }

    private fun extractAddress(scanResult: Any?): String? {
        return try {
            val deviceMethod = scanResult?.javaClass?.declaredMethods?.firstOrNull {
                it.name == "a" && it.returnType.name == "android.bluetooth.BluetoothDevice"
            }
            val device = deviceMethod?.invoke(scanResult)
            device?.javaClass?.getDeclaredMethod("getAddress")?.invoke(device) as? String
        } catch (e: Throwable) { null }
    }

    private fun extractName(scanResult: Any?): String? {
        return try {
            val deviceMethod = scanResult?.javaClass?.declaredMethods?.firstOrNull {
                it.name == "a" && it.returnType.name == "android.bluetooth.BluetoothDevice"
            }
            val device = deviceMethod?.invoke(scanResult)
            device?.javaClass?.getDeclaredMethod("getName")?.invoke(device) as? String
        } catch (e: Throwable) { null }
    }

    private fun extractRssi(scanResult: Any?): Int? {
        return try {
            val rssiMethod = scanResult?.javaClass?.declaredMethods?.firstOrNull {
                it.name == "c" && it.returnType == Int::class.javaPrimitiveType
            }
            rssiMethod?.invoke(scanResult) as? Int
        } catch (e: Throwable) { null }
    }
}

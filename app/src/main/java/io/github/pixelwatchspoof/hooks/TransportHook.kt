package io.github.pixelwatchspoof.hooks

import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.pixelwatchspoof.config.DeviceConfig

object TransportHook {

    fun hook(module: XposedModule, classLoader: ClassLoader) {
        hookL2Packet(module, classLoader)
        hookNearbyService(module, classLoader)
        hookSystemClient(module, classLoader)
        hookCapabilityManager(module, classLoader)
        hookScanFilter(module, classLoader)
    }

    private fun hookL2Packet(module: XposedModule, classLoader: ClassLoader) {
        val clazz = try {
            Class.forName(
                "com.xiaomi.wearable.transport.layerl2.L2Packet", true, classLoader
            )
        } catch (_: Throwable) {
            return
        }

        for (ctor in clazz.declaredConstructors) {
            if (ctor.parameterTypes.size == 4) {
                module.hook(ctor).intercept { chain ->
                    val channel = chain.args[0]
                    val opCode = chain.args[1]
                    val payload = chain.args[3] as? ByteArray

                    module.log(
                        Log.DEBUG, DeviceConfig.TAG,
                        "L2Packet: channel=$channel, opCode=$opCode, " +
                        "payloadSize=${payload?.size ?: 0}"
                    )

                    chain.proceed()
                }
            }
        }
    }

    private fun hookNearbyService(module: XposedModule, classLoader: ClassLoader) {
        val clazz = try {
            Class.forName("com.xiaomi.wearable.NearbyService", true, classLoader)
        } catch (_: Throwable) {
            return
        }

        for (method in clazz.declaredMethods) {
            if (method.name.contains("createBond", ignoreCase = true) ||
                method.name.contains("bind", ignoreCase = true)
            ) {
                module.hook(method).intercept { chain ->
                    val mac = chain.args.firstOrNull() as? String
                    module.log(
                        Log.INFO, DeviceConfig.TAG,
                        "NearbyService.${method.name}: mac=$mac"
                    )
                    chain.proceed()
                }
            }
        }
    }

    private fun hookSystemClient(module: XposedModule, classLoader: ClassLoader) {
        val clazz = try {
            Class.forName("com.xiaomi.wearable.spp.SystemClient", true, classLoader)
        } catch (_: Throwable) {
            return
        }

        for (ctor in clazz.declaredConstructors) {
            if (ctor.parameterTypes.size == 2) {
                module.hook(ctor).intercept { chain ->
                    val addr = chain.args[0] as? String
                    val bondWithoutDialog = chain.args[1] as? Boolean
                    module.log(
                        Log.INFO, DeviceConfig.TAG,
                        "SystemClient init: addr=$addr, bondWithoutDialog=$bondWithoutDialog"
                    )
                    chain.proceed()
                }
            }
        }

        try {
            val connectAfterBond = clazz.getDeclaredMethod(
                "connectAfterBond",
                Class.forName("android.bluetooth.BluetoothDevice", false, classLoader)
            )
            module.hook(connectAfterBond).intercept { chain ->
                val device = chain.args[0]
                val deviceName = try {
                    device.javaClass.getMethod("getName").invoke(device) as? String
                } catch (_: Throwable) { "unknown" }
                val deviceAddr = try {
                    device.javaClass.getMethod("getAddress").invoke(device) as? String
                } catch (_: Throwable) { "unknown" }

                module.log(
                    Log.INFO, DeviceConfig.TAG,
                    "SystemClient.connectAfterBond: name=$deviceName, addr=$deviceAddr"
                )

                chain.proceed()
            }
        } catch (_: Throwable) {}
    }

    private fun hookCapabilityManager(module: XposedModule, classLoader: ClassLoader) {
        val clazz = try {
            Class.forName("com.xiaomi.wearable.core.CapabilityManager", true, classLoader)
        } catch (_: Throwable) {
            return
        }

        for (method in clazz.declaredMethods) {
            val name = method.name
            if (name.contains("capability", ignoreCase = true) ||
                name.contains("Capability", ignoreCase = true)
            ) {
                module.hook(method).intercept { chain ->
                    module.log(
                        Log.DEBUG, DeviceConfig.TAG,
                        "CapabilityManager.$name called"
                    )
                    chain.proceed()
                }
            }
        }
    }

    private fun hookScanFilter(module: XposedModule, classLoader: ClassLoader) {
        try {
            val scanFilterBuilderClass = Class.forName(
                "android.bluetooth.le.ScanFilter.Builder", false, classLoader
            )
            val setManufacturerData = scanFilterBuilderClass.getDeclaredMethod(
                "setManufacturerData",
                Int::class.java,
                ByteArray::class.java,
                ByteArray::class.java
            )
            module.hook(setManufacturerData).intercept { chain ->
                val companyId = chain.args[0] as Int
                val data = chain.args[1] as? ByteArray
                val mask = chain.args[2] as? ByteArray

                module.log(
                    Log.DEBUG, DeviceConfig.TAG,
                    "ScanFilter: companyId=$companyId, data=${data?.size ?: 0}b"
                )

                chain.proceed()
            }
        } catch (_: Throwable) {}
    }
}

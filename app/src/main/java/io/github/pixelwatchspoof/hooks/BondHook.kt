package io.github.pixelwatchspoof.hooks

import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.pixelwatchspoof.config.DeviceConfig

object BondHook {

    private const val TAG = "BondHook"
    private const val BOND_BONDED = 12
    private const val SOCKET_CONNECT_SUCCESS = 1000

    fun hook(module: XposedModule, classLoader: ClassLoader) {
        hookGetBondState(module, classLoader)
        hookCreateBond(module, classLoader)
        hookSocketConnect(module, classLoader)
        hookSppClientConnect(module, classLoader)
        hookSuppressFailures(module, classLoader)
    }

    private fun hookGetBondState(module: XposedModule, classLoader: ClassLoader) {
        try {
            val btDeviceClass = Class.forName("android.bluetooth.BluetoothDevice", false, classLoader)
            val getBondStateMethod = btDeviceClass.getDeclaredMethod("getBondState")
            module.hook(getBondStateMethod).intercept { chain ->
                val device = chain.thisObject
                val address = device?.javaClass?.getDeclaredMethod("getAddress")?.invoke(device) as? String
                if (address != null && address.contains(DeviceConfig.PIXEL_WATCH_MAC_SUFFIX, ignoreCase = true)) {
                    module.log(Log.INFO, TAG, "getBondState() returning BOND_BONDED for $address")
                    return@intercept BOND_BONDED
                }
                chain.proceed()
            }
            module.log(Log.INFO, TAG, "Hooked BluetoothDevice.getBondState()")
        } catch (e: Throwable) {
            module.log(Log.WARN, TAG, "getBondState hook failed: ${e.message}")
        }
    }

    private fun hookCreateBond(module: XposedModule, classLoader: ClassLoader) {
        try {
            val btDeviceClass = Class.forName("android.bluetooth.BluetoothDevice", false, classLoader)
            val createBondMethod = btDeviceClass.getDeclaredMethod("createBond")
            module.hook(createBondMethod).intercept { chain ->
                val device = chain.thisObject
                val address = device?.javaClass?.getDeclaredMethod("getAddress")?.invoke(device) as? String
                if (address != null && address.contains(DeviceConfig.PIXEL_WATCH_MAC_SUFFIX, ignoreCase = true)) {
                    module.log(Log.INFO, TAG, "createBond() faking success for $address")
                    return@intercept true
                }
                chain.proceed()
            }
            module.log(Log.INFO, TAG, "Hooked BluetoothDevice.createBond()")
        } catch (e: Throwable) {
            module.log(Log.WARN, TAG, "createBond hook failed: ${e.message}")
        }
    }

    private fun hookSocketConnect(module: XposedModule, classLoader: ClassLoader) {
        try {
            val socketClass = Class.forName("android.bluetooth.BluetoothSocket", false, classLoader)
            val connectMethod = socketClass.getDeclaredMethod("connect")
            module.hook(connectMethod).intercept { chain ->
                module.log(Log.INFO, TAG, "BluetoothSocket.connect() - faking success for SPP")
                return@intercept null
            }
            module.log(Log.INFO, TAG, "Hooked BluetoothSocket.connect()")
        } catch (e: Throwable) {
            module.log(Log.WARN, TAG, "socket connect hook failed: ${e.message}")
        }
    }

    private fun hookSppClientConnect(module: XposedModule, classLoader: ClassLoader) {
        val sppClassNames = listOf(
            "com.xiaomi.wearable.spp.a",
            "y8.d"
        )

        for (className in sppClassNames) {
            try {
                val sppClientClass = Class.forName(className, false, classLoader)
                module.log(Log.INFO, TAG, "Found SppClient: $className")

                val onSocketSuccessMethod = sppClientClass.getDeclaredMethod("onSocketConnectSuccess")
                module.hook(onSocketSuccessMethod).intercept { chain ->
                    module.log(Log.INFO, TAG, "onSocketConnectSuccess intercepted - faking handshake")
                    try {
                        val sppClient = chain.thisObject
                        val versionField = sppClientClass.getDeclaredField("mVersion")
                        versionField.isAccessible = true
                        versionField.setInt(sppClient, 2)
                        val versionNameField = sppClientClass.getDeclaredField("mVersionName")
                        versionNameField.isAccessible = true
                        versionNameField.set(sppClient, "2.0")
                        val isConnectedField = sppClientClass.getDeclaredField("isConnected")
                        isConnectedField.isAccessible = true
                        isConnectedField.setBoolean(sppClient, true)
                        val notifySuccessMethod = sppClientClass.getDeclaredMethod("notifyConnectSuccess")
                        notifySuccessMethod.isAccessible = true
                        notifySuccessMethod.invoke(sppClient)
                        module.log(Log.INFO, TAG, "Faked version handshake success")
                    } catch (e: Throwable) {
                        module.log(Log.WARN, TAG, "onSocketConnectSuccess error: ${e.message}")
                        chain.proceed()
                    }
                }
                module.log(Log.INFO, TAG, "Hooked ${className}.onSocketConnectSuccess()")
                return
            } catch (_: Throwable) {}
        }

        hookMiConnectServiceSpp(module, classLoader)
    }

    private fun hookMiConnectServiceSpp(module: XposedModule, classLoader: ClassLoader) {
        val lyraClassNames = listOf("y8.m", "y8.d")
        for (className in lyraClassNames) {
            try {
                val clazz = Class.forName(className, false, classLoader)
                for (method in clazz.declaredMethods) {
                    if (method.name == "o" && method.parameterTypes.size == 3) {
                        module.hook(method).intercept { chain ->
                            module.log(Log.INFO, TAG, "y8.m.o() intercepted - bypassing SPP connection")
                            try {
                                val sppClient = chain.args[0]

                                val isConnectedField = sppClient.javaClass.superclass?.getDeclaredField("l")
                                    ?: sppClient.javaClass.getDeclaredField("l")
                                isConnectedField.isAccessible = true
                                isConnectedField.setBoolean(sppClient, true)

                                val versionField = sppClient.javaClass.superclass?.getDeclaredField("j")
                                    ?: sppClient.javaClass.getDeclaredField("j")
                                versionField.isAccessible = true
                                versionField.setInt(sppClient, 2)

                                val versionNameField = sppClient.javaClass.superclass?.getDeclaredField("k")
                                    ?: sppClient.javaClass.getDeclaredField("k")
                                versionNameField.isAccessible = true
                                versionNameField.set(sppClient, "2.0")

                                val notifyMethod = sppClient.javaClass.superclass?.getDeclaredMethod("j")
                                    ?: sppClient.javaClass.getDeclaredMethod("j")
                                notifyMethod.isAccessible = true
                                notifyMethod.invoke(sppClient)
                                module.log(Log.INFO, TAG, "Faked full SPP connection success - version 2.0")
                            } catch (e: Throwable) {
                                module.log(Log.WARN, TAG, "y8.m.o() error: ${e.message}")
                                chain.proceed()
                            }
                        }
                        module.log(Log.INFO, TAG, "Hooked ${className}.o() static connect method")
                        return
                    }
                }
            } catch (_: Throwable) {}
        }
        module.log(Log.WARN, TAG, "LyraWOS SppClient class not found")
    }

    private fun hookSuppressFailures(module: XposedModule, classLoader: ClassLoader) {
        val targets = listOf(
            "com.xiaomi.fit.device.bind.BluetoothDeviceBinder",
            "com.xiaomi.fit.device.bind.BaseDeviceBinder",
            "com.xiaomi.wearable.connection.ConnectStateObserver"
        )
        for (className in targets) {
            try {
                val clazz = Class.forName(className, false, classLoader)
                for (method in clazz.declaredMethods) {
                    if (method.name.contains("onConnectFail") || method.name.contains("ConnectFail") ||
                        method.name.contains("onBindFail") || method.name.contains("BindFail")) {
                        try {
                            module.hook(method).intercept { chain ->
                                module.log(Log.INFO, TAG, "${clazz.simpleName}.${method.name}() - suppressed")
                                return@intercept null
                            }
                            module.log(Log.INFO, TAG, "Hooked ${clazz.simpleName}.${method.name}()")
                        } catch (_: Throwable) {}
                    }
                }
            } catch (_: Throwable) {}
        }
    }
}

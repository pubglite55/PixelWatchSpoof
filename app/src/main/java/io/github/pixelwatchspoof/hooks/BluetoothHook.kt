package io.github.pixelwatchspoof.hooks

import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.pixelwatchspoof.config.DeviceConfig
import io.github.pixelwatchspoof.config.DeviceConfig

object BluetoothHook {

    private const val TAG = "BluetoothHook"

    fun hook(module: XposedModule, packageName: String, classLoader: ClassLoader) {
        when (packageName) {
            "com.android.bluetooth" -> hookBluetoothProcess(module, classLoader)
            "com.xiaomi.bluetooth" -> hookXiaomiBluetooth(module, classLoader)
            "com.milink.service" -> hookMiLink(module, classLoader)
        }
    }

    private fun hookBluetoothProcess(module: XposedModule, classLoader: ClassLoader) {
        module.log(Log.INFO, TAG, "Hooking com.android.bluetooth")
        hookScanFilterMatches(module, classLoader)
    }

    private fun hookScanFilterMatches(module: XposedModule, classLoader: ClassLoader) {
        try {
            val scanFilterClass = Class.forName(
                "android.bluetooth.le.ScanFilter", false, classLoader
            )
            val scanResultClass = Class.forName(
                "android.bluetooth.le.ScanResult", false, classLoader
            )

            val matchesMethod = scanFilterClass.getDeclaredMethod("matches", scanResultClass)
            module.hook(matchesMethod).intercept { chain ->
                val original = chain.proceed() as Boolean
                if (original) return@intercept true

                try {
                    val scanResult = chain.args[0]
                    val deviceMethod = scanResultClass.getDeclaredMethod("getDevice")
                    val device = deviceMethod.invoke(scanResult)
                    val addressMethod = device?.javaClass?.getDeclaredMethod("getAddress")
                    val address = addressMethod?.invoke(device) as? String

                    if (address != null && isPixelWatchAddress(address)) {
                        module.log(Log.INFO, TAG, "Bypassing ScanFilter for Pixel Watch $address")
                        return@intercept true
                    }
                } catch (e: Throwable) {
                    // Silently fall through
                }

                original
            }
            module.log(Log.INFO, TAG, "Hooked ScanFilter.matches()")
        } catch (e: Throwable) {
            module.log(Log.WARN, TAG, "ScanFilter hook failed: ${e.message}")
            hookScanControllerFallback(module, classLoader)
        }
    }

    private fun hookScanControllerFallback(module: XposedModule, classLoader: ClassLoader) {
        try {
            val scanControllerClass = Class.forName(
                "com.android.bluetooth.le_scan.ScanController", false, classLoader
            )
            module.log(Log.INFO, TAG, "Found ScanController class (fallback)")

            val methods = scanControllerClass.declaredMethods.filter {
                it.name.contains("match") || it.name.contains("filter") || it.name.contains("check")
            }
            methods.forEach { m ->
                module.log(Log.INFO, TAG, "ScanController method: ${m.name}(${m.parameterTypes.joinToString { it.simpleName }})")
            }
        } catch (e: Throwable) {
            module.log(Log.WARN, TAG, "ScanController fallback failed: ${e.message}")
        }
    }

    private fun isPixelWatchAddress(address: String): Boolean {
        return address.contains(DeviceConfig.PIXEL_WATCH_MAC_SUFFIX, ignoreCase = true)
    }

    private fun hookXiaomiBluetooth(module: XposedModule, classLoader: ClassLoader) {
        module.log(Log.INFO, TAG, "Hooking com.xiaomi.bluetooth")
    }

    private fun hookMiLink(module: XposedModule, classLoader: ClassLoader) {
        module.log(Log.INFO, TAG, "Hooking com.milink.service")
    }
}

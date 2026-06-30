package io.github.pixelwatchspoof.config

object DeviceConfig {

    const val TAG = "PixelWatchSpoof"

    const val TARGET_PACKAGE = "com.mi.health"

    // Your Pixel Watch's MAC address suffix (last 2 bytes)
    // Find it via: Settings > About Phone > Bluetooth Address
    const val PIXEL_WATCH_MAC_SUFFIX = "5B:85"

    // Unique device ID for this Pixel Watch (arbitrary string)
    const val PIXEL_WATCH_DID = "pixel_watch_035t"

    // Full MAC address of your Pixel Watch
    const val PIXEL_WATCH_MAC = "D4:3A:2C:72:5B:85"

    object XiaomiWatch5 {
        const val MODEL = "midr.watch.m62s"
        const val PRODUCT_NAME = "Xiaomi Watch 5"
        const val PRODUCT_ID = "xiaomi.watch.5"
        const val DEVICE_NAME = "Xiaomi Watch 5"
        const val TYPE = 1  // TYPE_WEAROS = 1, not BLE_WATCH = 2
        const val ACCESS_TYPE = 1  // DualCoreWearOS (accessType == 1)
        const val REGION = "CN"
        const val REQUEST_BOND = true
        const val CREATE_BOND_WITHOUT_DIALOG = true
        const val PROXY_CONNECTION_FIRST = true
        const val IS_WEAR_OS = true
        const val IS_THIRD_PARTY = false
        const val IS_DUAL_CORE_WEAR_OS = true
    }

    val SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB"

    val GATT_SERVICE_UUID = "0000fe95-0000-1000-8000-00805f9b34fb"
    val GATT_CHAR_PROTO_TX = "00002a06-0000-1000-8000-00805f9b34fb"
    val GATT_CHAR_PROTO_RX = "00002a04-0000-1000-8000-00805f9b34fb"

    val XPOSED_PACKAGES = setOf(
        "de.robv.android.xposed.installer",
        "com.saurik.substrate",
        "org.meowcat.edxposed.manager",
        "org.lsposed.manager",
        "io.github.libxposed.manager",
        "io.github.lsposed.manager"
    )

    val HIDDEN_FILE_PATHS = setOf(
        "/system/framework/XposedBridge.jar",
        "/system/lib/libxposed_art.so",
        "/system/lib64/libxposed_art.so",
        "/data/misc/riru",
        "/data/adb/lspd",
        "/data/adb/modules"
    )
}

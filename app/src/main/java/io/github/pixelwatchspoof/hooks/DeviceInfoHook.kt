package io.github.pixelwatchspoof.hooks

import io.github.libxposed.api.XposedModule
import io.github.pixelwatchspoof.config.DeviceConfig

object DeviceInfoHook {

    fun hook(module: XposedModule, classLoader: ClassLoader) {
        hookDeviceInfoConstructor(module, classLoader)
        hookDeviceInfoGetters(module, classLoader)
        hookProductExt(module, classLoader)
        hookDeviceInfoToString(module, classLoader)
    }

    private fun hookDeviceInfoConstructor(module: XposedModule, classLoader: ClassLoader) {
        val clazz = Class.forName(
            "com.xiaomi.wearable.core.DeviceInfo", true, classLoader
        )

        for (ctor in clazz.declaredConstructors) {
            val paramTypes = ctor.parameterTypes
            if (paramTypes.size < 10) continue

            module.hook(ctor).intercept { chain ->
                val args = chain.args.toMutableList()

                if (args.size > 3 && args[3] is String?) {
                    args[3] = DeviceConfig.XiaomiWatch5.MODEL
                }
                if (args.size > 4 && args[4] is Int) {
                    args[4] = DeviceConfig.XiaomiWatch5.TYPE
                }
                if (args.size > 6 && args[6] is String?) {
                    args[6] = DeviceConfig.XiaomiWatch5.PRODUCT_NAME
                }
                if (args.size > 7 && args[7] is String?) {
                    args[7] = DeviceConfig.XiaomiWatch5.PRODUCT_ID
                }
                if (args.size > 8 && args[8] is String?) {
                    args[8] = DeviceConfig.XiaomiWatch5.DEVICE_NAME
                }
                if (args.size > 9 && args[9] is Int) {
                    args[9] = DeviceConfig.XiaomiWatch5.ACCESS_TYPE
                }
                if (args.size > 11 && args[11] is Boolean) {
                    args[11] = DeviceConfig.XiaomiWatch5.REQUEST_BOND
                }
                if (args.size > 15 && args[15] is String?) {
                    args[15] = DeviceConfig.XiaomiWatch5.REGION
                }
                if (args.size > 17 && args[17] is Boolean) {
                    args[17] = DeviceConfig.XiaomiWatch5.CREATE_BOND_WITHOUT_DIALOG
                }
                if (args.size > 18 && args[18] is Boolean) {
                    args[18] = DeviceConfig.XiaomiWatch5.PROXY_CONNECTION_FIRST
                }

                chain.proceed(args.toTypedArray())
            }
        }
    }

    private fun hookDeviceInfoGetters(module: XposedModule, classLoader: ClassLoader) {
        val clazz = Class.forName(
            "com.xiaomi.wearable.core.DeviceInfo", true, classLoader
        )

        hookGetter(module, clazz, "getModel") { DeviceConfig.XiaomiWatch5.MODEL }
        hookGetter(module, clazz, "getProductName") { DeviceConfig.XiaomiWatch5.PRODUCT_NAME }
        hookGetter(module, clazz, "getProductId") { DeviceConfig.XiaomiWatch5.PRODUCT_ID }
        hookGetter(module, clazz, "getDeviceName") { DeviceConfig.XiaomiWatch5.DEVICE_NAME }
        hookGetter(module, clazz, "getType") { DeviceConfig.XiaomiWatch5.TYPE }
        hookGetter(module, clazz, "getRegion") { DeviceConfig.XiaomiWatch5.REGION }
        hookGetter(module, clazz, "getAccessType") { DeviceConfig.XiaomiWatch5.ACCESS_TYPE }
        hookGetter(module, clazz, "getRequestBond") { DeviceConfig.XiaomiWatch5.REQUEST_BOND }
        hookGetter(module, clazz, "getCreateBondWithoutDialog") { DeviceConfig.XiaomiWatch5.CREATE_BOND_WITHOUT_DIALOG }
        hookGetter(module, clazz, "getProxyConnectionFirst") { DeviceConfig.XiaomiWatch5.PROXY_CONNECTION_FIRST }
        hookGetter(module, clazz, "isThirdParty") { DeviceConfig.XiaomiWatch5.IS_THIRD_PARTY }
        hookGetter(module, clazz, "isWearOs") { DeviceConfig.XiaomiWatch5.IS_WEAR_OS }

        hookGetter(module, clazz, "isBleWatch") { false }
        hookGetter(module, clazz, "isBleBand") { false }
        hookGetter(module, clazz, "isBle") { false }
        hookGetter(module, clazz, "isWatch") { true }
        hookGetter(module, clazz, "isBand") { false }
        hookGetter(module, clazz, "isDual") { false }
        hookGetter(module, clazz, "isDualWatch") { false }
        hookGetter(module, clazz, "isDualBand") { false }
        hookGetter(module, clazz, "isHuaMi") { false }
        hookGetter(module, clazz, "isEarphone") { false }
        hookGetter(module, clazz, "isEcg") { false }
        hookGetter(module, clazz, "isBloodSugar") { false }
        hookGetter(module, clazz, "isJumpScale") { false }
        hookGetter(module, clazz, "isWearableDevice") { true }
        hookGetter(module, clazz, "isDualCoreWearOS") { true }
    }

    private fun hookProductExt(module: XposedModule, classLoader: ClassLoader) {
        val productClass = try {
            Class.forName("com.xiaomi.fitness.device.manager.bean.Product", true, classLoader)
        } catch (_: Throwable) {
            return
        }

        val extClass = try {
            Class.forName(
                "com.xiaomi.fit.device.extensions.ProductExtKt", true, classLoader
            )
        } catch (_: Throwable) {
            return
        }

        for (method in extClass.declaredMethods) {
            val name = method.name
            val params = method.parameterTypes
            if (params.size == 1 && params[0] == productClass) {
                when (name) {
                    "isWearOS" -> module.hook(method).intercept { true }
                    "isBle" -> module.hook(method).intercept { false }
                    "isDualCoreWearOS" -> module.hook(method).intercept { true }
                    "isThirdParty" -> module.hook(method).intercept { false }
                    "isEarphone" -> module.hook(method).intercept { false }
                    "isHuaMi" -> module.hook(method).intercept { false }
                    "isDual" -> module.hook(method).intercept { false }
                    "isJumpScale" -> module.hook(method).intercept { false }
                    "isBloodSugar" -> module.hook(method).intercept { false }
                    "isRequestBond" -> module.hook(method).intercept { true }
                    "createBondWithoutDialog" -> module.hook(method).intercept { true }
                    "isSupportSppProxy" -> module.hook(method).intercept { true }
                    "isSupportBleProxy" -> module.hook(method).intercept { true }
                }
            }
        }
    }

    private fun hookDeviceInfoToString(module: XposedModule, classLoader: ClassLoader) {
        val clazz = Class.forName(
            "com.xiaomi.wearable.core.DeviceInfo", true, classLoader
        )
        val toString = clazz.getDeclaredMethod("toString")
        module.hook(toString).intercept { chain ->
            val obj = chain.thisObject
            val addr = obj.javaClass.getMethod("getAddress").invoke(obj) as? String ?: "??"
            "DeviceInfo(address=$addr, model=${DeviceConfig.XiaomiWatch5.MODEL}, type=${DeviceConfig.XiaomiWatch5.TYPE}, deviceName=${DeviceConfig.XiaomiWatch5.DEVICE_NAME})"
        }
    }

    private fun hookGetter(
        module: XposedModule,
        clazz: Class<*>,
        methodName: String,
        value: () -> Any?
    ) {
        try {
            val method = clazz.getDeclaredMethod(methodName)
            module.hook(method).intercept { value() }
        } catch (_: Throwable) {}
    }
}

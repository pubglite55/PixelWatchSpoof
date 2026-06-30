package io.github.pixelwatchspoof.hooks

import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.pixelwatchspoof.config.DeviceConfig

object AuthHook {

    fun hook(module: XposedModule, classLoader: ClassLoader) {
        hookWearAuthV2(module, classLoader)
        hookCipherApiCall(module, classLoader)
    }

    private fun hookWearAuthV2(module: XposedModule, classLoader: ClassLoader) {
        val clazz = try {
            Class.forName("com.xiaomi.wearable.wear.api.WearAuthV2", true, classLoader)
        } catch (_: Throwable) {
            module.log(4, DeviceConfig.TAG, "WearAuthV2 class not found")
            return
        }

        for (method in clazz.declaredMethods) {
            when (method.name) {
                "j" -> {
                    if (method.parameterTypes.size == 3 &&
                        method.parameterTypes.all { it == ByteArray::class.java }
                    ) {
                        module.hook(method).intercept { chain ->
                            val appRandom = chain.args[0] as ByteArray
                            val deviceRandom = chain.args[1] as ByteArray
                            val deviceSign = chain.args[2] as ByteArray

                            module.log(
                                Log.INFO, DeviceConfig.TAG,
                                "WearAuth verify: " +
                                "appRandom=${toHex(appRandom)}, " +
                                "deviceRandom=${toHex(deviceRandom)}, " +
                                "deviceSign=${toHex(deviceSign)}"
                            )

                            chain.proceed()
                        }
                    }
                }
                "i" -> {
                    if (method.parameterTypes.isEmpty()) {
                        module.hook(method).intercept { chain ->
                            val authObj = chain.thisObject

                            try {
                                val oField = authObj.javaClass.getDeclaredField("o")
                                oField.isAccessible = true
                                val currentVal = oField.get(authObj)
                                module.log(
                                    Log.INFO, DeviceConfig.TAG,
                                    "sendAppVerify: current userId field = $currentVal"
                                )
                            } catch (e: Throwable) {
                                module.log(
                                    Log.WARN, DeviceConfig.TAG,
                                    "sendAppVerify: cannot read userId field: ${e.message}"
                                )
                            }

                            chain.proceed()
                        }
                    }
                }
                "h" -> {
                    if (method.parameterTypes.size == 2 &&
                        method.parameterTypes.all { it == ByteArray::class.java }
                    ) {
                        module.hook(method).intercept { chain ->
                            val appSign = chain.args[0] as ByteArray
                            val encryptedInfo = chain.args[1] as ByteArray

                            module.log(
                                Log.INFO, DeviceConfig.TAG,
                                "sendAppConfirm: sign=${toHex(appSign)}, " +
                                "encryptedInfo=${toHex(encryptedInfo)}"
                            )

                            val result = chain.proceed()

                            val authObj = chain.thisObject
                            try {
                                val callbackField = authObj.javaClass.superclass
                                    ?.getDeclaredField("g") ?: return@intercept result
                                callbackField.isAccessible = true
                                val callback = callbackField.get(authObj)
                                if (callback != null) {
                                    val successMethod = callback.javaClass.getMethod(
                                        "b", Int::class.java, String::class.java
                                    )
                                    successMethod.invoke(callback, 2004, "device ready")
                                    module.log(
                                        Log.INFO, DeviceConfig.TAG,
                                        "sendAppConfirm: forced success callback"
                                    )
                                }
                            } catch (e: Throwable) {
                                module.log(
                                    Log.WARN, DeviceConfig.TAG,
                                    "sendAppConfirm: cannot force callback: ${e.message}"
                                )
                            }

                            result
                        }
                    }
                }
            }
        }
    }

    private fun hookCipherApiCall(module: XposedModule, classLoader: ClassLoader) {
        val clazz = try {
            Class.forName("com.xiaomi.wearable.wear.api.CipherApiCall", true, classLoader)
        } catch (_: Throwable) {
            return
        }

        try {
            val updateKeys = clazz.getDeclaredMethod(
                "updateKeys",
                ByteArray::class.java, ByteArray::class.java,
                ByteArray::class.java, ByteArray::class.java
            )
            module.hook(updateKeys).intercept { chain ->
                val deviceKey = chain.args[0] as ByteArray
                val appKey = chain.args[1] as ByteArray
                val deviceIV = chain.args[2] as ByteArray
                val appIV = chain.args[3] as ByteArray

                module.log(
                    Log.INFO, DeviceConfig.TAG,
                    "updateKeys: deviceKey=${toHex(deviceKey)}, " +
                    "appKey=${toHex(appKey)}, " +
                    "deviceIV=${toHex(deviceIV)}, " +
                    "appIV=${toHex(appIV)}"
                )

                chain.proceed()
            }
        } catch (_: Throwable) {}
    }

    private fun toHex(bytes: ByteArray): String {
        if (bytes.size > 16) {
            return bytes.take(8).joinToString("") { "%02x".format(it) } +
                   "...(${bytes.size}b)"
        }
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

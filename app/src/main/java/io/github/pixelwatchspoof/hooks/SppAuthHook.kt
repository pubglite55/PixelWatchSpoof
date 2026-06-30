package io.github.pixelwatchspoof.hooks

import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.pixelwatchspoof.config.DeviceConfig

object SppAuthHook {

    private const val TAG = "SppAuthHook"

    fun hook(module: XposedModule, classLoader: ClassLoader) {
        hookGetBindInfo(module, classLoader)
        hookRedirectFailureToSuccess(module, classLoader)
        hookRequestDevice(module, classLoader)
        hookServerBind(module, classLoader)
    }

    private fun findField(obj: Any, fieldName: String): java.lang.reflect.Field? {
        var c: Class<*>? = obj.javaClass
        while (c != null) {
            try {
                val f = c.getDeclaredField(fieldName)
                f.isAccessible = true
                return f
            } catch (_: NoSuchFieldException) { c = c.superclass }
        }
        return null
    }

    private fun hookGetBindInfo(module: XposedModule, classLoader: ClassLoader) {
        val targets = listOf(
            "com.xiaomi.fit.device.bind.WearBinderV2",
            "com.xiaomi.fit.device.bind.LocalWearBinderV2"
        )
        for (className in targets) {
            try {
                val clazz = Class.forName(className, false, classLoader)
                val method = clazz.declaredMethods.firstOrNull {
                    it.name == "i" && it.parameterTypes.isEmpty()
                } ?: continue

                module.hook(method).intercept { chain ->
                    module.log(Log.INFO, TAG, ">>> getBindInfo intercepted in $className")
                    val binder = chain.thisObject
                    val cbField = findField(binder, "mCallback") ?: return@intercept chain.proceed()
                    val target = cbField.get(binder) ?: return@intercept chain.proceed()

                    module.log(Log.INFO, TAG, "Found mCallback: ${target.javaClass.name}")
                    val model = DeviceConfig.XiaomiWatch5.MODEL
                    val did = "pixel_watch_035t"
                    val mac = "D4:3A:2C:72:5B:85"
                    val onSuccessMethod = target.javaClass.methods.firstOrNull {
                        it.name == "onBindSuccess" && it.parameterTypes.size == 4
                    }
                    if (onSuccessMethod != null) {
                        module.log(Log.INFO, TAG, "Calling onBindSuccess on binder")
                        onSuccessMethod.invoke(target, model, did, mac, null)
                        module.log(Log.INFO, TAG, "onBindSuccess delivered! Restarting app in 2s...")
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            try {
                                Runtime.getRuntime().exec(arrayOf("am", "force-stop", "com.mi.health"))
                                module.log(Log.INFO, TAG, "App force-stopped")
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                    try {
                                        Runtime.getRuntime().exec(arrayOf("am", "start", "-n", "com.mi.health/com.xiaomi.fitness.login.SplashActivity"))
                                        module.log(Log.INFO, TAG, "App relaunched")
                                    } catch (e: Throwable) {
                                        module.log(Log.WARN, TAG, "Relaunch failed: ${e.message}")
                                    }
                                }, 1500)
                            } catch (e: Throwable) {
                                module.log(Log.WARN, TAG, "Failed: ${e.message}")
                            }
                        }, 2000)
                    } else {
                        module.log(Log.WARN, TAG, "onBindSuccess not found")
                    }
                    return@intercept null
                }
                module.log(Log.INFO, TAG, "Hooked $className.i()")
            } catch (e: Throwable) {
                module.log(Log.WARN, TAG, "$className hook failed: ${e.message}")
            }
        }
    }

    private fun hookRequestDevice(module: XposedModule, classLoader: ClassLoader) {
        try {
            val clazz = Class.forName("com.xiaomi.fit.device.bind.BaseDeviceBinder", false, classLoader)
            val method = clazz.getDeclaredMethod("requestDevice", String::class.java)
            module.hook(method).intercept { chain ->
                val binder = chain.thisObject
                module.log(Log.INFO, TAG, "requestDevice intercepted")
                val did = findField(binder, "mDid")?.get(binder) as? String ?: "pixel_watch_035t"
                val isRebind = findField(binder, "isRebind")?.getBoolean(binder) ?: false
                try {
                    val getMgrMethod = binder.javaClass.methods.firstOrNull {
                        it.name == "getMDeviceManager" && it.parameterTypes.isEmpty()
                    }
                    val mgr = getMgrMethod?.invoke(binder)
                    if (mgr != null) {
                        val notifyMethod = mgr.javaClass.methods.firstOrNull {
                            it.name == "notifyDeviceBind" && it.parameterTypes.size == 2
                        }
                        notifyMethod?.invoke(mgr, did, isRebind)
                        module.log(Log.INFO, TAG, "notifyDeviceBind($did) called")
                    }
                } catch (e: Throwable) {
                    module.log(Log.WARN, TAG, "notifyDeviceBind failed: ${e.message}")
                }
                module.log(Log.INFO, TAG, "Skipped onRequestDeviceSuccess to avoid GATT connection attempt")
                return@intercept null
            }
            module.log(Log.INFO, TAG, "Hooked BaseDeviceBinder.requestDevice()")
        } catch (e: Throwable) {
            module.log(Log.WARN, TAG, "requestDevice hook failed: ${e.message}")
        }
    }

    private fun hookServerBind(module: XposedModule, classLoader: ClassLoader) {
        // Hook BindToServer.bindWithSign to fake server registration success
        try {
            val clazz = Class.forName("com.xiaomi.fit.device.bind.BindToServer", false, classLoader)
            for (method in clazz.declaredMethods) {
                if (method.name == "bindWithSign") {
                    module.hook(method).intercept { chain ->
                        module.log(Log.INFO, TAG, "bindWithSign intercepted - faking server success")
                        val args = chain.args
                        if (args.size >= 2) {
                            val onSuccess = args[1]
                            if (onSuccess != null) {
                                try {
                                    val invokeMethod = onSuccess.javaClass.methods.firstOrNull {
                                        it.name == "invoke" && it.parameterTypes.size == 1
                                    }
                                    if (invokeMethod != null) {
                                        invokeMethod.invoke(onSuccess, true)
                                        module.log(Log.INFO, TAG, "bindWithSign success callback invoked!")
                                    }
                                } catch (e: Throwable) {
                                    module.log(Log.WARN, TAG, "Failed to invoke success: ${e.message}")
                                }
                            }
                        }
                        return@intercept null
                    }
                    module.log(Log.INFO, TAG, "Hooked BindToServer.bindWithSign()")
                }
            }
        } catch (e: Throwable) {
            module.log(Log.WARN, TAG, "BindToServer hook failed: ${e.message}")
        }

        // Also hook the AIDL unauthCall to fake success for bind-related calls
        try {
            val clazz = Class.forName("com.xiaomi.wearable.core.client.IMiWearCoreClient", false, classLoader)
            val method = clazz.declaredMethods.firstOrNull {
                it.name == "unauthCall" && it.parameterTypes.size >= 3
            }
            if (method != null) {
                module.hook(method).intercept { chain ->
                    val callback = chain.args.lastOrNull { it.javaClass.name.contains("ICallback") || it.javaClass.interfaces?.any { i -> i.name.contains("ICallback") } == true }
                    if (callback != null) {
                        module.log(Log.INFO, TAG, "unauthCall intercepted, faking WearApiResult success")
                        try {
                            val wearApiResultClass = Class.forName("com.xiaomi.wearable.core.WearApiResult", false, classLoader)
                            val constructor = wearApiResultClass.declaredConstructors.firstOrNull {
                                it.parameterTypes.size == 2 && it.parameterTypes[0] == Int::class.javaPrimitiveType
                            }
                            if (constructor != null) {
                                val result = constructor.newInstance(0, ByteArray(0)) // code=0 means success
                                val callbackMethod = callback.javaClass.methods.firstOrNull {
                                    it.name == "onCallback" && it.parameterTypes.size == 1
                                }
                                if (callbackMethod != null) {
                                    callbackMethod.invoke(callback, result)
                                    module.log(Log.INFO, TAG, "unauthCall success callback invoked!")
                                }
                            }
                        } catch (e: Throwable) {
                            module.log(Log.WARN, TAG, "unauthCall fake failed: ${e.message}")
                        }
                    }
                    chain.proceed()
                }
                module.log(Log.INFO, TAG, "Hooked unauthCall()")
            }
        } catch (e: Throwable) {
            module.log(Log.WARN, TAG, "unauthCall hook failed: ${e.message}")
        }
    }

    private fun hookRedirectFailureToSuccess(module: XposedModule, classLoader: ClassLoader) {
        try {
            val clazz = Class.forName("com.xiaomi.fit.device.bind.BluetoothDeviceBinder", false, classLoader)
            val method = clazz.getDeclaredMethod("onBindFailure", Int::class.java, String::class.java)
            module.hook(method).intercept { chain ->
                val binder = chain.thisObject
                module.log(Log.INFO, TAG, "onBindFailure(${chain.args[0]}, ${chain.args[1]}) -> success")
                val diField = findField(binder, "mDeviceInfo")
                val deviceInfo = diField?.get(binder)
                if (deviceInfo != null) {
                    val macAddr = deviceInfo.javaClass.getMethod("getMac").invoke(deviceInfo) as? String
                    if (macAddr != null) {
                        try {
                            val wc = Class.forName("com.xiaomi.wearable.core.client.IMiWearCoreClient", false, classLoader)
                            val inst = wc.getField("Companion").get(null).javaClass.getMethod("getInstance").invoke(wc.getField("Companion").get(null))
                            inst.javaClass.getMethod("closeUnauthConnection", String::class.java).invoke(inst, macAddr)
                        } catch (_: Throwable) {}
                    }
                }
                val did = "pixel_watch_035t"
                findField(binder, "mDid")?.set(binder, did)
                findField(binder, "isBindSuccess")?.setBoolean(binder, true)
                val cb = findField(binder, "callback")?.get(binder)
                cb?.javaClass?.methods?.firstOrNull {
                    it.name == "onBindSuccess" && it.parameterTypes.size == 1
                }?.invoke(cb, did)
                module.log(Log.INFO, TAG, "callback.onBindSuccess($did) called")
                return@intercept null
            }
            module.log(Log.INFO, TAG, "Hooked BluetoothDeviceBinder.onBindFailure()")
        } catch (e: Throwable) {
            module.log(Log.WARN, TAG, "onBindFailure hook failed: ${e.message}")
        }
    }
}

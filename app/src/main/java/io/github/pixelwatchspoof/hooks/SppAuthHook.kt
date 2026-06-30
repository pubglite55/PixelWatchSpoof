package io.github.pixelwatchspoof.hooks

import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.pixelwatchspoof.config.DeviceConfig

object SppAuthHook {

    private const val TAG = "SppAuthHook"

    fun hook(module: XposedModule, classLoader: ClassLoader) {
        hookGetBindInfo(module, classLoader)
        hookBindSuccess(module, classLoader)
        hookConnectDevice(module, classLoader)
        hookBindFailure(module, classLoader)
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
                    val did = DeviceConfig.PIXEL_WATCH_DID
                    val mac = DeviceConfig.PIXEL_WATCH_MAC
                    val onSuccessMethod = target.javaClass.methods.firstOrNull {
                        it.name == "onBindSuccess" && it.parameterTypes.size == 4
                    }
                    if (onSuccessMethod != null) {
                        module.log(Log.INFO, TAG, "Calling onBindSuccess on binder")
                        onSuccessMethod.invoke(target, model, did, mac, null)
                        module.log(Log.INFO, TAG, "onBindSuccess delivered!")
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

    private fun hookBindSuccess(module: XposedModule, classLoader: ClassLoader) {
        try {
            val clazz = Class.forName("com.xiaomi.fit.device.bind.BaseDeviceBinder", false, classLoader)
            val method = clazz.getDeclaredMethod("onBindSuccess",
                String::class.java, String::class.java, String::class.java, String::class.java)
            module.hook(method).intercept { chain ->
                val binder = chain.thisObject
                val model = chain.args[0] as String
                val did = chain.args[1] as String
                val mac = (chain.args[2] as String).uppercase(java.util.Locale.ROOT)
                val sn = chain.args[3] as? String

                module.log(Log.INFO, TAG, "onBindSuccess intercepted: model=$model, did=$did, mac=$mac")

                // Set internal state
                findField(binder, "mDid")?.set(binder, did)
                findField(binder, "sn")?.set(binder, sn)
                findField(binder, "isBindSuccess")?.setBoolean(binder, true)

                // Remove device binder from factory
                try {
                    val factoryClass = Class.forName("com.xiaomi.fit.device.DeviceFactory", false, classLoader)
                    val instance = factoryClass.getField("INSTANCE").get(null)
                    val removeMethod = instance.javaClass.methods.firstOrNull {
                        it.name == "removeDeviceBinder" && it.parameterTypes.size == 1
                    }
                    removeMethod?.invoke(instance, mac)
                    module.log(Log.INFO, TAG, "Removed device binder for $mac")
                } catch (e: Throwable) {
                    module.log(Log.WARN, TAG, "removeDeviceBinder failed: ${e.message}")
                }

                // Report bind success
                try {
                    val reportMethod = binder.javaClass.methods.firstOrNull {
                        it.name == "reportBindSuccess" && it.parameterTypes.isEmpty()
                    }
                    reportMethod?.invoke(binder)
                    module.log(Log.INFO, TAG, "reportBindSuccess called")
                } catch (e: Throwable) {
                    module.log(Log.WARN, TAG, "reportBindSuccess failed: ${e.message}")
                }

                // Set preferences
                try {
                    val prefClass = Class.forName("com.xiaomi.fitness.device.manager.DevicePreference", false, classLoader)
                    val prefInstance = prefClass.getField("INSTANCE").get(null)
                    val setMethod = prefInstance.javaClass.methods.firstOrNull {
                        it.name == "setIN_BIND_NEW_MODE" && it.parameterTypes.size == 1
                    }
                    setMethod?.invoke(prefInstance, false)
                } catch (_: Throwable) {}

                // Remove did from account
                try {
                    val accountClass = Class.forName("com.xiaomi.fitness.account.extensions.AccountManagerExtKt", false, classLoader)
                    val userInfoClass = Class.forName("com.xiaomi.fitness.account.user.UserInfoManager", false, classLoader)
                    val userInfoInstance = userInfoClass.getField("INSTANCE").get(null)
                    val getInstanceMethod = accountClass.methods.firstOrNull {
                        it.name == "getInstance" && it.parameterTypes.size == 1
                    }
                    val accountManager = getInstanceMethod?.invoke(null, userInfoInstance)
                    if (accountManager != null) {
                        val removeDidMethod = accountManager.javaClass.methods.firstOrNull {
                            it.name == "removeDid" && it.parameterTypes.size == 1
                        }
                        removeDidMethod?.invoke(accountManager, did)
                    }
                } catch (_: Throwable) {}

                // Call callback.onBindSuccess(did) to notify UI
                try {
                    val callbackField = findField(binder, "callback")
                    val callback = callbackField?.get(binder)
                    if (callback != null) {
                        val cbMethod = callback.javaClass.methods.firstOrNull {
                            it.name == "onBindSuccess" && it.parameterTypes.size == 1
                        }
                        cbMethod?.invoke(callback, did)
                        module.log(Log.INFO, TAG, "callback.onBindSuccess($did) called")
                    }
                } catch (e: Throwable) {
                    module.log(Log.WARN, TAG, "callback.onBindSuccess failed: ${e.message}")
                }

                // Call callback.onRequestDeviceSuccess() to trigger device list refresh
                try {
                    val callbackField = findField(binder, "callback")
                    val callback = callbackField?.get(binder)
                    if (callback != null) {
                        val cbMethod = callback.javaClass.methods.firstOrNull {
                            it.name == "onRequestDeviceSuccess" && it.parameterTypes.isEmpty()
                        }
                        cbMethod?.invoke(callback)
                        module.log(Log.INFO, TAG, "callback.onRequestDeviceSuccess() called")
                    }
                } catch (e: Throwable) {
                    module.log(Log.WARN, TAG, "callback.onRequestDeviceSuccess failed: ${e.message}")
                }

                module.log(Log.INFO, TAG, "onBindSuccess fully handled - skipping server calls")

                // Schedule closing the stuck bind activity via am
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try {
                        Runtime.getRuntime().exec(arrayOf("am", "start", "-n", "com.mi.health/.main.MainActivity", "--activity-clear-top"))
                        module.log(Log.INFO, TAG, "Launched MainActivity to close bind activity")
                    } catch (e: Throwable) {
                        module.log(Log.WARN, TAG, "Failed to launch MainActivity: ${e.message}")
                    }
                }, 3000)

                return@intercept null
            }
            module.log(Log.INFO, TAG, "Hooked BaseDeviceBinder.onBindSuccess()")
        } catch (e: Throwable) {
            module.log(Log.WARN, TAG, "onBindSuccess hook failed: ${e.message}")
        }
    }

    private fun hookConnectDevice(module: XposedModule, classLoader: ClassLoader) {
        try {
            val clazz = Class.forName("com.xiaomi.fitness.device.manager.remote.DeviceManagerRemoteImpl", false, classLoader)
            val method = clazz.declaredMethods.firstOrNull {
                it.name == "connectDevice" && it.parameterTypes.size >= 2
            }
            if (method != null) {
                module.hook(method).intercept { chain ->
                    module.log(Log.INFO, TAG, "connectDevice intercepted - skipping GATT connection")
                    return@intercept null
                }
                module.log(Log.INFO, TAG, "Hooked DeviceManagerRemoteImpl.connectDevice()")
            }
        } catch (e: Throwable) {
            module.log(Log.WARN, TAG, "connectDevice hook failed: ${e.message}")
        }
    }

    private fun hookBindFailure(module: XposedModule, classLoader: ClassLoader) {
        try {
            val clazz = Class.forName("com.xiaomi.fit.device.bind.BluetoothDeviceBinder", false, classLoader)
            val method = clazz.getDeclaredMethod("onBindFailure", Int::class.java, String::class.java)
            module.hook(method).intercept { chain ->
                val binder = chain.thisObject
                val code = chain.args[0] as Int
                val message = chain.args[1] as? String
                module.log(Log.INFO, TAG, "onBindFailure($code, $message) -> redirecting to success")

                // Close unauth connection
                try {
                    val diField = findField(binder, "mDeviceInfo")
                    val deviceInfo = diField?.get(binder)
                    if (deviceInfo != null) {
                        val macAddr = deviceInfo.javaClass.getMethod("getMac").invoke(deviceInfo) as? String
                        if (macAddr != null) {
                            val wc = Class.forName("com.xiaomi.wearable.core.client.IMiWearCoreClient", false, classLoader)
                            val inst = wc.getField("Companion").get(null).javaClass.getMethod("getInstance").invoke(wc.getField("Companion").get(null))
                            inst.javaClass.getMethod("closeUnauthConnection", String::class.java).invoke(inst, macAddr)
                        }
                    }
                } catch (_: Throwable) {}

                val did = DeviceConfig.PIXEL_WATCH_DID
                findField(binder, "mDid")?.set(binder, did)
                findField(binder, "isBindSuccess")?.setBoolean(binder, true)

                // Call callback.onBindSuccess(did)
                try {
                    val cbField = findField(binder, "callback")
                    val cb = cbField?.get(binder)
                    if (cb != null) {
                        val m = cb.javaClass.methods.firstOrNull {
                            it.name == "onBindSuccess" && it.parameterTypes.size == 1
                        }
                        m?.invoke(cb, did)
                        module.log(Log.INFO, TAG, "callback.onBindSuccess($did) called")
                    }
                } catch (e: Throwable) {
                    module.log(Log.WARN, TAG, "callback.onBindSuccess failed: ${e.message}")
                }

                // Call callback.onRequestDeviceSuccess()
                try {
                    val cbField = findField(binder, "callback")
                    val cb = cbField?.get(binder)
                    if (cb != null) {
                        val m = cb.javaClass.methods.firstOrNull {
                            it.name == "onRequestDeviceSuccess" && it.parameterTypes.isEmpty()
                        }
                        m?.invoke(cb)
                        module.log(Log.INFO, TAG, "callback.onRequestDeviceSuccess() called")
                    }
                } catch (e: Throwable) {
                    module.log(Log.WARN, TAG, "callback.onRequestDeviceSuccess failed: ${e.message}")
                }

                return@intercept null
            }
            module.log(Log.INFO, TAG, "Hooked BluetoothDeviceBinder.onBindFailure()")
        } catch (e: Throwable) {
            module.log(Log.WARN, TAG, "onBindFailure hook failed: ${e.message}")
        }
    }

    /**
     * Hook BindBleDeviceDeviceActivity to finish itself after bind success.
     * The activity is stuck at "正在连接设备..." because the ViewModel's did is null.
     * We detect when onBindSuccess is called and finish the activity after a delay.
     */
}

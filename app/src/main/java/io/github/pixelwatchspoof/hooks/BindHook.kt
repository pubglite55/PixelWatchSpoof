package io.github.pixelwatchspoof.hooks

import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.pixelwatchspoof.config.DeviceConfig

object BindHook {

    fun hook(module: XposedModule, classLoader: ClassLoader) {
        hookBindToServer(module, classLoader)
        hookLocalBind(module, classLoader)
    }

    private fun hookBindToServer(module: XposedModule, classLoader: ClassLoader) {
        val clazz = try {
            Class.forName("com.xiaomi.fit.device.bind.BindToServer", true, classLoader)
        } catch (_: Throwable) {
            module.log(4, DeviceConfig.TAG, "BindToServer not found")
            return
        }

        for (method in clazz.declaredMethods) {
            when (method.name) {
                "applyBind" -> hookApplyBind(module, method)
                "confirmBind" -> hookConfirmBind(module, method)
                "verifyBind" -> hookVerifyBind(module, method)
                "unbind" -> hookUnbind(module, method)
                "recordToServer" -> hookRecordToServer(module, method)
            }
        }
    }

    private fun hookApplyBind(module: XposedModule, method: java.lang.reflect.Method) {
        module.hook(method).intercept { chain ->
            val args = chain.args.toMutableList()

            for (i in args.indices) {
                if (args[i] is String) {
                    val s = args[i] as String
                    if (s.contains("watch", ignoreCase = true) ||
                        s.contains("band", ignoreCase = true) ||
                        s.contains("wear", ignoreCase = true)
                    ) {
                        module.log(
                            Log.INFO, DeviceConfig.TAG,
                            "applyBind arg[$i]: '$s' -> '${DeviceConfig.XiaomiWatch5.MODEL}'"
                        )
                        args[i] = DeviceConfig.XiaomiWatch5.MODEL
                    }
                }
            }

            chain.proceed(args.toTypedArray())
        }
    }

    private fun hookConfirmBind(module: XposedModule, method: java.lang.reflect.Method) {
        module.hook(method).intercept { chain ->
            val args = chain.args.toMutableList()

            for (i in args.indices) {
                if (args[i] is String) {
                    val s = args[i] as String
                    if (s.contains("watch", ignoreCase = true) ||
                        s.contains("band", ignoreCase = true)
                    ) {
                        module.log(
                            Log.INFO, DeviceConfig.TAG,
                            "confirmBind arg[$i]: '$s' -> '${DeviceConfig.XiaomiWatch5.MODEL}'"
                        )
                        args[i] = DeviceConfig.XiaomiWatch5.MODEL
                    }
                }
            }

            chain.proceed(args.toTypedArray())
        }
    }

    private fun hookVerifyBind(module: XposedModule, method: java.lang.reflect.Method) {
        module.hook(method).intercept { chain ->
            val args = chain.args.toMutableList()

            for (i in args.indices) {
                if (args[i] is String) {
                    val s = args[i] as String
                    if (s.contains("watch", ignoreCase = true) ||
                        s.contains("band", ignoreCase = true)
                    ) {
                        args[i] = DeviceConfig.XiaomiWatch5.MODEL
                    }
                }
            }

            chain.proceed(args.toTypedArray())
        }
    }

    private fun hookUnbind(module: XposedModule, method: java.lang.reflect.Method) {
        module.hook(method).intercept { chain ->
            val args = chain.args.toMutableList()
            for (i in args.indices) {
                if (args[i] is String) {
                    val s = args[i] as String
                    if (s.contains("watch", ignoreCase = true) ||
                        s.contains("band", ignoreCase = true)
                    ) {
                        args[i] = DeviceConfig.XiaomiWatch5.MODEL
                    }
                }
            }
            chain.proceed(args.toTypedArray())
        }
    }

    private fun hookRecordToServer(module: XposedModule, method: java.lang.reflect.Method) {
        module.hook(method).intercept { chain ->
            val args = chain.args.toMutableList()
            for (i in args.indices) {
                if (args[i] is String) {
                    val s = args[i] as String
                    if (s.contains("watch", ignoreCase = true) ||
                        s.contains("band", ignoreCase = true)
                    ) {
                        args[i] = DeviceConfig.XiaomiWatch5.MODEL
                    }
                }
            }
            chain.proceed(args.toTypedArray())
        }
    }

    private fun hookLocalBind(module: XposedModule, classLoader: ClassLoader) {
        val clazz = try {
            Class.forName("com.xiaomi.fit.device.bind.BindToLocal", true, classLoader)
        } catch (_: Throwable) {
            return
        }

        for (method in clazz.declaredMethods) {
            if (method.name.contains("bind", ignoreCase = true) ||
                method.name.contains("model", ignoreCase = true)
            ) {
                module.hook(method).intercept { chain ->
                    val args = chain.args.toMutableList()
                    for (i in args.indices) {
                        if (args[i] is String) {
                            val s = args[i] as String
                            if (s.contains("watch", ignoreCase = true) ||
                                s.contains("band", ignoreCase = true)
                            ) {
                                args[i] = DeviceConfig.XiaomiWatch5.MODEL
                            }
                        }
                    }
                    chain.proceed(args.toTypedArray())
                }
            }
        }
    }
}

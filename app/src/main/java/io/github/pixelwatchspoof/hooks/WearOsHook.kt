package io.github.pixelwatchspoof.hooks

import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.pixelwatchspoof.config.DeviceConfig

object WearOsHook {

    fun hook(module: XposedModule, classLoader: ClassLoader) {
        hookWearOsUtil(module, classLoader)
        hookFastPairManager(module, classLoader)
        hookWearOsApi(module, classLoader)
        hookWearOsExt(module, classLoader)
        hookConnectivityCompanionDeviceService(module, classLoader)
    }

    private fun hookWearOsUtil(module: XposedModule, classLoader: ClassLoader) {
        val clazz = try {
            Class.forName("com.xiaomi.fitness.wearos.export.WearOSUtil", true, classLoader)
        } catch (_: Throwable) {
            module.log(4, DeviceConfig.TAG, "WearOSUtil interface not found")
            return
        }

        for (method in clazz.declaredMethods) {
            when (method.name) {
                "launchBindWearOSPage" -> {
                    module.hook(method).intercept { chain ->
                        val args = chain.args.toMutableList()
                        module.log(
                            Log.INFO, DeviceConfig.TAG,
                            "WearOSUtil.launchBindWearOSPage called, args size=${args.size}"
                        )
                        for (i in args.indices) {
                            module.log(Log.DEBUG, DeviceConfig.TAG, "  arg[$i]: ${args[i]?.javaClass?.simpleName} = ${args[i]}")
                        }
                        chain.proceed(args.toTypedArray())
                    }
                }
                "associate" -> {
                    module.hook(method).intercept { chain ->
                        val nodeId = chain.args.firstOrNull()
                        module.log(
                            Log.INFO, DeviceConfig.TAG,
                            "WearOSUtil.associate: nodeId=$nodeId"
                        )
                        chain.proceed()
                    }
                }
                "enableConnection" -> {
                    module.hook(method).intercept { chain ->
                        val peerId = chain.args.firstOrNull()
                        module.log(
                            Log.INFO, DeviceConfig.TAG,
                            "WearOSUtil.enableConnection: peerId=$peerId"
                        )
                        chain.proceed()
                    }
                }
                "disableConnection" -> {
                    module.hook(method).intercept { chain ->
                        val peerId = chain.args.firstOrNull()
                        module.log(
                            Log.INFO, DeviceConfig.TAG,
                            "WearOSUtil.disableConnection: peerId=$peerId"
                        )
                        chain.proceed()
                    }
                }
                "isLocalPaired" -> {
                    module.hook(method).intercept { chain ->
                        val nodeId = chain.args.firstOrNull() as? String
                        module.log(
                            Log.INFO, DeviceConfig.TAG,
                            "WearOSUtil.isLocalPaired: nodeId=$nodeId"
                        )
                        val result = chain.proceed()
                        module.log(
                            Log.DEBUG, DeviceConfig.TAG,
                            "WearOSUtil.isLocalPaired result=$result"
                        )
                        result
                    }
                }
                "isUnderOobe" -> {
                    module.hook(method).intercept { chain ->
                        module.log(Log.INFO, DeviceConfig.TAG, "WearOSUtil.isUnderOobe called")
                        chain.proceed()
                    }
                }
                "isWearosRequirementNotPass" -> {
                    module.hook(method).intercept { chain ->
                        module.log(Log.INFO, DeviceConfig.TAG, "WearOSUtil.isWearosRequirementNotPass called")
                        false  // Always return false to pass requirements
                    }
                }
                "refreshPairedWatch" -> {
                    module.hook(method).intercept { chain ->
                        module.log(Log.INFO, DeviceConfig.TAG, "WearOSUtil.refreshPairedWatch called")
                        chain.proceed()
                    }
                }
                "unbind" -> {
                    module.hook(method).intercept { chain ->
                        val str = chain.args.firstOrNull() as? String
                        module.log(
                            Log.INFO, DeviceConfig.TAG,
                            "WearOSUtil.unbind: str=$str"
                        )
                        chain.proceed()
                    }
                }
            }
        }
    }

    private fun hookFastPairManager(module: XposedModule, classLoader: ClassLoader) {
        val clazz = try {
            Class.forName("com.xiaomi.fitness.wearos.export.FastPairManager", true, classLoader)
        } catch (_: Throwable) {
            module.log(4, DeviceConfig.TAG, "FastPairManager interface not found")
            return
        }

        for (method in clazz.declaredMethods) {
            when (method.name) {
                "maybeStartFastPairing" -> {
                    module.hook(method).intercept { chain ->
                        val context = chain.args[0]
                        val productId = chain.args[1]
                        val productName = chain.args[2]
                        module.log(
                            Log.INFO, DeviceConfig.TAG,
                            "FastPairManager.maybeStartFastPairing: productId=$productId, productName=$productName"
                        )
                        chain.proceed()
                    }
                }
                "saveFastPairingExtra" -> {
                    module.hook(method).intercept { chain ->
                        module.log(Log.INFO, DeviceConfig.TAG, "FastPairManager.saveFastPairingExtra called")
                        chain.proceed()
                    }
                }
                "getFastPairExtras" -> {
                    module.hook(method).intercept { chain ->
                        module.log(Log.INFO, DeviceConfig.TAG, "FastPairManager.getFastPairExtras called")
                        chain.proceed()
                    }
                }
                "removeFastPairExtras" -> {
                    module.hook(method).intercept { chain ->
                        module.log(Log.INFO, DeviceConfig.TAG, "FastPairManager.removeFastPairExtras called")
                        chain.proceed()
                    }
                }
            }
        }
    }

    private fun hookWearOsApi(module: XposedModule, classLoader: ClassLoader) {
        val clazz = try {
            Class.forName("com.xiaomi.fitness.wearos.export.WearOSApi", true, classLoader)
        } catch (_: Throwable) {
            module.log(4, DeviceConfig.TAG, "WearOSApi interface not found")
            return
        }

        for (method in clazz.declaredMethods) {
            when (method.name) {
                "bindDevice" -> {
                    module.hook(method).intercept { chain ->
                        module.log(Log.INFO, DeviceConfig.TAG, "WearOSApi.bindDevice called")
                        chain.proceed()
                    }
                }
                "isPhoneSwitching" -> {
                    module.hook(method).intercept { chain ->
                        module.log(Log.INFO, DeviceConfig.TAG, "WearOSApi.isPhoneSwitching called")
                        false
                    }
                }
            }
        }
    }

    private fun hookWearOsExt(module: XposedModule, classLoader: ClassLoader) {
        val clazz = try {
            Class.forName("com.xiaomi.fitness.wearos.export.WearOsExtKt", true, classLoader)
        } catch (_: Throwable) {
            return
        }

        for (method in clazz.declaredMethods) {
            module.log(Log.DEBUG, DeviceConfig.TAG, "WearOsExtKt method: ${method.name}")
        }
    }

    private fun hookConnectivityCompanionDeviceService(module: XposedModule, classLoader: ClassLoader) {
        val clazz = try {
            Class.forName("com.xiaomi.fitness.wearos.export.ConnectivityCompanionDeviceService", true, classLoader)
        } catch (_: Throwable) {
            return
        }

        for (method in clazz.declaredMethods) {
            if (method.name.contains("onDevice", ignoreCase = true) ||
                method.name.contains("appear", ignoreCase = true) ||
                method.name.contains("disappear", ignoreCase = true)
            ) {
                module.hook(method).intercept { chain ->
                    module.log(
                        Log.INFO, DeviceConfig.TAG,
                        "ConnectivityCompanionDeviceService.${method.name} called"
                    )
                    chain.proceed()
                }
            }
        }
    }
}

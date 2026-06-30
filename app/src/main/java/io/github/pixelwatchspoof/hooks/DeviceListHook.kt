package io.github.pixelwatchspoof.hooks

import android.os.IBinder
import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.pixelwatchspoof.config.DeviceConfig

object DeviceListHook {

    private const val TAG = "DeviceListHook"
    private const val WEAROS_DEVICE_TYPE = 1

    private var injected = false

    fun hook(module: XposedModule, classLoader: ClassLoader) {
        hookBinderPoolLazy(module, classLoader)
    }

    private fun hookBinderPoolLazy(module: XposedModule, classLoader: ClassLoader) {
        try {
            val poolClass = Class.forName(
                "com.xiaomi.fitness.service.DefaultBinderPool",
                false,
                classLoader
            )

            val getBindersMethod = poolClass.getDeclaredMethod("getBinders")
            module.hook(getBindersMethod).intercept { chain ->
                val binders = chain.proceed()

                if (binders != null && !injected) {
                    val bindersMap = binders as? MutableMap<Int, IBinder>
                    if (bindersMap != null && !bindersMap.containsKey(WEAROS_DEVICE_TYPE)) {
                        module.log(Log.INFO, TAG, "Injecting dummy binder for WearOS type=$WEAROS_DEVICE_TYPE")
                        bindersMap[WEAROS_DEVICE_TYPE] = android.os.Binder()
                        injected = true
                        module.log(Log.INFO, TAG, "Dummy binder injected successfully")
                    }
                }

                binders
            }
            module.log(Log.INFO, TAG, "Hooked DefaultBinderPool.getBinders()")
        } catch (e: Throwable) {
            module.log(Log.WARN, TAG, "DefaultBinderPool hook failed: ${e.message}")
        }
    }
}

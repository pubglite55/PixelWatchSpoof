package io.github.pixelwatchspoof.hooks

import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.pixelwatchspoof.config.DeviceConfig

object ProductHook {

    private const val TAG = "ProductHook"
    private const val TARGET_PRODUCT_ID = 20863

    fun hook(module: XposedModule, classLoader: ClassLoader) {
        hookScanManagerBypass(module, classLoader)
        hookDeviceProcessProduct(module, classLoader)
        dumpWearableProducts(module, classLoader)
    }

    private fun dumpWearableProducts(module: XposedModule, classLoader: ClassLoader) {
        try {
            val productClass = Class.forName(
                "com.xiaomi.fitness.device.manager.export.bean.WearableProduct",
                false, classLoader
            )
            val superFields = productClass.superclass?.declaredFields?.map { it.name } ?: emptyList()
            module.log(Log.INFO, TAG, "WearableProduct fields: $superFields")

            val allFields = productClass.superclass?.superclass?.declaredFields?.map { it.name } ?: emptyList()
            module.log(Log.INFO, TAG, "Product fields: $allFields")
        } catch (e: Throwable) {
            module.log(Log.WARN, TAG, "dumpProducts failed: ${e.message}")
        }
    }

    private fun hookScanManagerBypass(module: XposedModule, classLoader: ClassLoader) {
        try {
            val scanMgrClass = Class.forName(
                "com.xiaomi.fitness.device.manager.ui.scan.ScanManager",
                false, classLoader
            )
            val bMethod = scanMgrClass.declaredMethods.firstOrNull {
                it.name == "b" && it.parameterTypes.size == 2 &&
                it.parameterTypes[0].simpleName == "SearchCallback" &&
                it.parameterTypes[1].simpleName == "BleDevice"
            }
            if (bMethod != null) {
                module.hook(bMethod).intercept { chain ->
                    val bleDevice = chain.args[1]
                    val address = getBleDeviceAddress(bleDevice, classLoader)
                    if (address != null && address.contains("5B:85", ignoreCase = true)) {
                        module.log(Log.INFO, TAG, "ScanManager.b bypass for Pixel Watch")
                        invokeOnFound(chain.args[0], bleDevice, classLoader, module)
                        return@intercept null
                    }
                    chain.proceed()
                }
                module.log(Log.INFO, TAG, "Hooked ScanManager.b bypass")
            }
        } catch (e: Throwable) {
            module.log(Log.WARN, TAG, "ScanManager hook failed: ${e.message}")
        }
    }

    private fun hookDeviceProcessProduct(module: XposedModule, classLoader: ClassLoader) {
        val knownClasses = listOf(
            "com.xiaomi.fitness.device.manager.j",
            "com.xiaomi.fitness.device.manager.i",
            "com.xiaomi.fitness.device.manager.k",
            "com.xiaomi.fitness.device.manager.DeviceManagerRemoteImpl",
            "com.xiaomi.fitness.device.manager.DeviceManagerImpl",
            "com.xiaomi.fitness.device.manager.DeviceModel"
        )

        for (name in knownClasses) {
            try {
                val clazz = Class.forName(name, false, classLoader)
                hookClassMethods(module, clazz, classLoader)
            } catch (_: Throwable) {}
        }

        hookByMethodSearch(module, classLoader)
    }

    private fun hookClassMethods(module: XposedModule, clazz: Class<*>, classLoader: ClassLoader) {
        val allMethods = try { clazz.methods } catch (_: Throwable) { clazz.declaredMethods }
        for (method in allMethods) {
            if (method.name == "getProductByModel" && method.parameterTypes.size == 1 &&
                !java.lang.reflect.Modifier.isAbstract(method.modifiers)) {
                try {
                    module.hook(method).intercept { chain ->
                        val original = chain.proceed()
                        if (original != null) return@intercept original
                        val model = chain.args[0] as? String
                        if (model == DeviceConfig.XiaomiWatch5.MODEL) {
                            module.log(Log.INFO, TAG, "getProductByModel injecting in ${clazz.simpleName}")
                            createFakeProduct(module, classLoader)
                        } else null
                    }
                    module.log(Log.INFO, TAG, "Hooked ${clazz.name}.getProductByModel()")
                } catch (e: Throwable) {
                    module.log(Log.WARN, TAG, "hook ${clazz.name}.getProductByModel failed: ${e.message}")
                }
            }

            if (method.name == "findProduct" && method.parameterTypes.size == 1 &&
                !java.lang.reflect.Modifier.isAbstract(method.modifiers)) {
                try {
                    module.hook(method).intercept { chain ->
                        val original = chain.proceed()
                        if (original != null) return@intercept original
                        try {
                            val predicate = chain.args[0] ?: return@intercept null
                            val fakeProduct = createFakeProduct(module, classLoader)
                                ?: return@intercept null
                            val invokeMethod = predicate.javaClass.methods.firstOrNull {
                                it.name == "invoke" && it.parameterTypes.size == 1
                            } ?: return@intercept null
                            val result = invokeMethod.invoke(predicate, fakeProduct) as? Boolean
                            if (result == true) {
                                module.log(Log.INFO, TAG, "findProduct injecting in ${clazz.simpleName}")
                                return@intercept fakeProduct
                            }
                        } catch (_: Throwable) {}
                        null
                    }
                    module.log(Log.INFO, TAG, "Hooked ${clazz.name}.findProduct()")
                } catch (e: Throwable) {
                    module.log(Log.WARN, TAG, "hook ${clazz.name}.findProduct failed: ${e.message}")
                }
            }
        }
    }

    private fun hookByMethodSearch(module: XposedModule, classLoader: ClassLoader) {
        val knownPrefixes = listOf(
            "com.xiaomi.fitness.device.manager.",
            "com.xiaomi.fitness.device.model."
        )

        val knownSuffixes = listOf(
            "Impl", "Manager", "Remote", "Model", "Service",
            "RemoteImpl", "ManagerImpl"
        )

        val triedClasses = HashSet<String>()

        for (prefix in knownPrefixes) {
            for (suffix in knownSuffixes) {
                val className = "$prefix${suffix}"
                if (className in triedClasses) continue
                triedClasses.add(className)

                try {
                    val clazz = Class.forName(className, false, classLoader)
                    hookClassMethods(module, clazz, classLoader)
                } catch (_: Throwable) {}
            }
        }

        for (i in 'a'..'z') {
            for (j in 'a'..'z') {
                val suffix = "$i$j"
                for (prefix in knownPrefixes) {
                    val className = "$prefix$suffix"
                    if (className in triedClasses) continue
                    triedClasses.add(className)

                    try {
                        val clazz = Class.forName(className, false, classLoader)
                        val allM = try { clazz.methods } catch (_: Throwable) { clazz.declaredMethods }
                        val hasTarget = allM.any {
                            it.name == "getProductByModel" || it.name == "findProduct"
                        }
                        if (hasTarget) {
                            module.log(Log.INFO, TAG, "Found product class: $className")
                            hookClassMethods(module, clazz, classLoader)
                        }
                    } catch (_: Throwable) {}
                }
            }
        }
    }

    private fun invokeOnFound(searchCallback: Any, bleDevice: Any, classLoader: ClassLoader, module: XposedModule) {
        try {
            val callbackClass = Class.forName(
                "com.xiaomi.fitness.device.manager.ui.scan.SearchCallback",
                false, classLoader
            )
            val onFoundField = callbackClass.getDeclaredField("onFound")
            onFoundField.isAccessible = true
            val onFound = onFoundField.get(searchCallback) ?: return
            val invokeMethod = onFound.javaClass.methods.firstOrNull {
                it.name == "invoke" && it.parameterTypes.size == 1
            } ?: return
            invokeMethod.invoke(onFound, bleDevice)
            module.log(Log.INFO, TAG, "Called onFound for Pixel Watch")
        } catch (e: Throwable) {
            module.log(Log.WARN, TAG, "invokeOnFound failed: ${e.message}")
        }
    }

    private fun getBleDeviceAddress(bleDevice: Any?, classLoader: ClassLoader): String? {
        if (bleDevice == null) return null
        return try {
            val clazz = Class.forName(
                "com.xiaomi.fitness.device.manager.export.scan.BleDevice",
                false, classLoader
            )
            clazz.getDeclaredMethod("getAddress").invoke(bleDevice) as? String
        } catch (_: Throwable) {
            try {
                val field = bleDevice.javaClass.declaredFields.firstOrNull { it.name == "address" }
                    ?: bleDevice.javaClass.superclass?.declaredFields?.firstOrNull { it.name == "address" }
                field?.isAccessible = true
                field?.get(bleDevice) as? String
            } catch (_: Throwable) { null }
        }
    }

    private fun createFakeProduct(module: XposedModule, classLoader: ClassLoader): Any? {
        return try {
            val productClass = Class.forName(
                "com.xiaomi.fitness.device.manager.export.bean.WearableProduct",
                false, classLoader
            )
            val constructor = productClass.declaredConstructors.firstOrNull {
                it.parameterTypes.isEmpty()
            } ?: return null
            val instance = constructor.newInstance()
            setFields(instance, productClass)
            instance
        } catch (e: Throwable) {
            module.log(Log.WARN, TAG, "createFakeProduct failed: ${e.message}")
            null
        }
    }

    private fun setFields(instance: Any, clazz: Class<*>) {
        val fieldValues = mapOf(
            "productId" to TARGET_PRODUCT_ID,
            "model" to DeviceConfig.XiaomiWatch5.MODEL,
            "productName" to DeviceConfig.XiaomiWatch5.DEVICE_NAME,
            "status" to 1,
            "type" to 1,
            "accessType" to 1
        )
        for ((fieldName, value) in fieldValues) {
            var current: Class<*>? = clazz
            while (current != null) {
                try {
                    val field = current.getDeclaredField(fieldName)
                    field.isAccessible = true
                    when (value) {
                        is Int -> field.setInt(instance, value)
                        is String -> field.set(instance, value)
                    }
                    break
                } catch (_: NoSuchFieldException) { current = current.superclass }
            }
        }
    }
}

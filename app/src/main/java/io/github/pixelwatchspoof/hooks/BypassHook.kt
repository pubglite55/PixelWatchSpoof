package io.github.pixelwatchspoof.hooks

import android.content.pm.PackageManager
import io.github.libxposed.api.XposedModule
import io.github.pixelwatchspoof.config.DeviceConfig

object BypassHook {

    fun hook(module: XposedModule, classLoader: ClassLoader) {
        hookPackageManager(module, classLoader)
        hookFileExists(module, classLoader)
        hookSystemProperties(module, classLoader)
        hookStackTrace(module, classLoader)
    }

    private fun hookPackageManager(module: XposedModule, classLoader: ClassLoader) {
        val pmClass = try {
            Class.forName("android.app.ApplicationPackageManager", false, classLoader)
        } catch (_: Throwable) {
            Class.forName("android.app.ApplicationPackageManager")
        }

        try {
            val getPackageInfo = pmClass.getDeclaredMethod(
                "getPackageInfo", String::class.java, Int::class.java
            )
            module.hook(getPackageInfo).intercept { chain ->
                val pkg = chain.args[0] as String
                if (pkg in DeviceConfig.XPOSED_PACKAGES) {
                    throw PackageManager.NameNotFoundException("Package not found: $pkg")
                }
                chain.proceed()
            }
        } catch (e: Throwable) {
            module.log(4, DeviceConfig.TAG, "hook getPackageInfo failed: ${e.message}")
        }

        try {
            val getInstalledPackages = pmClass.getDeclaredMethod(
                "getInstalledPackages", Int::class.java
            )
            module.hook(getInstalledPackages).intercept { chain ->
                val result = chain.proceed()
                try {
                    val listField = result.javaClass.getField("list")
                    @Suppress("UNCHECKED_CAST")
                    val list = listField.get(result) as? List<*> ?: return@intercept result
                    val filtered = list.filter { pkg ->
                        val info = pkg?.javaClass?.getMethod("getPackageName")?.invoke(pkg) as? String
                        info !in DeviceConfig.XPOSED_PACKAGES
                    }
                    listField.set(result, filtered)
                } catch (_: Throwable) {}
                result
            }
        } catch (e: Throwable) {
            module.log(4, DeviceConfig.TAG, "hook getInstalledPackages failed: ${e.message}")
        }

        try {
            val resolveService = pmClass.getDeclaredMethod(
                "resolveService",
                android.content.Intent::class.java,
                Int::class.java
            )
            module.hook(resolveService).intercept { chain ->
                val intent = chain.args[0] as? android.content.Intent
                val comp = intent?.component
                if (comp?.packageName in DeviceConfig.XPOSED_PACKAGES) {
                    null
                } else {
                    chain.proceed()
                }
            }
        } catch (_: Throwable) {}
    }

    private fun hookFileExists(module: XposedModule, classLoader: ClassLoader) {
        val fileClass = Class.forName("java.io.File", false, classLoader)
        val existsMethod = fileClass.getDeclaredMethod("exists")

        module.hook(existsMethod).intercept { chain ->
            val file = chain.thisObject as java.io.File
            val path = file.absolutePath
            if (DeviceConfig.HIDDEN_FILE_PATHS.any { path.startsWith(it) }) {
                false
            } else {
                chain.proceed()
            }
        }

        val listFilesMethod = fileClass.getDeclaredMethod("listFiles")
        module.hook(listFilesMethod).intercept { chain ->
            val result = chain.proceed() as? Array<java.io.File>
            result?.filter { file ->
                DeviceConfig.HIDDEN_FILE_PATHS.none { file.absolutePath.startsWith(it) }
            }?.toTypedArray()
        }
    }

    private fun hookSystemProperties(module: XposedModule, classLoader: ClassLoader) {
        try {
            val systemClass = Class.forName("android.os.SystemProperties", false, classLoader)
            val getMethod = systemClass.getDeclaredMethod("get", String::class.java, String::class.java)

            module.hook(getMethod).intercept { chain ->
                val key = chain.args[0] as String
                when (key) {
                    "ro.debuggable" -> "0"
                    "ro.secure" -> "1"
                    "ro.build.type" -> "user"
                    "ro.build.tags" -> "release-keys"
                    "persist.sys.dalvik.vm.lib.2" -> "libart.so"
                    else -> chain.proceed()
                }
            }
        } catch (_: Throwable) {
            try {
                val systemClass = Class.forName("java.lang.System", false, classLoader)
                val getProperty = systemClass.getDeclaredMethod("getProperty", String::class.java)
                module.hook(getProperty).intercept { chain ->
                    val key = chain.args[0] as String
                    when (key) {
                        "ro.debuggable" -> "0"
                        "ro.secure" -> "1"
                        else -> chain.proceed()
                    }
                }
            } catch (_: Throwable) {}
        }
    }

    private fun hookStackTrace(module: XposedModule, classLoader: ClassLoader) {
        try {
            val threadClass = Class.forName("java.lang.Thread", false, classLoader)
            val getStackTrace = threadClass.getDeclaredMethod("getStackTrace")
            module.hook(getStackTrace).intercept { chain ->
                val stackTrace = chain.proceed() as? Array<StackTraceElement>
                stackTrace?.filter { element ->
                    val className = element.className
                    !className.contains("xposed", ignoreCase = true) &&
                    !className.contains("XposedBridge", ignoreCase = true) &&
                    !className.contains("lsposed", ignoreCase = true) &&
                    !className.contains("de.robv", ignoreCase = true)
                }?.toTypedArray()
            }
        } catch (_: Throwable) {}
    }
}

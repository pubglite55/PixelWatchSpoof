package io.github.pixelwatchspoof;

import android.util.Log;

import io.github.libxposed.api.XposedModule;
import io.github.pixelwatchspoof.config.DeviceConfig;
import io.github.pixelwatchspoof.hooks.AuthHook;
import io.github.pixelwatchspoof.hooks.BypassHook;
import io.github.pixelwatchspoof.hooks.DeviceInfoHook;
import io.github.pixelwatchspoof.hooks.BindHook;
import io.github.pixelwatchspoof.hooks.TransportHook;
import io.github.pixelwatchspoof.hooks.ScanHook;
import io.github.pixelwatchspoof.hooks.BluetoothHook;
import io.github.pixelwatchspoof.hooks.WearOsHook;
import io.github.pixelwatchspoof.hooks.DeviceListHook;
import io.github.pixelwatchspoof.hooks.ProductHook;
import io.github.pixelwatchspoof.hooks.BondHook;
import io.github.pixelwatchspoof.hooks.SppAuthHook;

public class HookEntry extends XposedModule {

    public HookEntry() {
        super();
    }

    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
        log(Log.INFO, DeviceConfig.TAG, "PixelWatchSpoof module loaded");
        log(Log.INFO, DeviceConfig.TAG, "Target: Pixel Watch 1 -> Xiaomi Watch 5");
        log(Log.INFO, DeviceConfig.TAG, "Framework: " + getFrameworkName() + " (" + getFrameworkVersionCode() + ")");
    }

    @Override
    public void onPackageLoaded(PackageLoadedParam param) {
        String pkg = param.getPackageName();
        ClassLoader cl = param.getDefaultClassLoader();

        if ("com.mi.health".equals(pkg)) {
            hookMiHealth(cl);
        } else if ("com.xiaomi.mi_connect_service".equals(pkg)) {
            hookMiConnectService(cl);
        } else if ("com.android.bluetooth".equals(pkg) ||
                   "com.xiaomi.bluetooth".equals(pkg) ||
                   "com.milink.service".equals(pkg)) {
            hookBluetooth(pkg, cl);
        }
    }

    private void hookMiHealth(ClassLoader cl) {
        log(Log.INFO, DeviceConfig.TAG, "Hooking com.mi.health");

        try {
            BypassHook.INSTANCE.hook(this, cl);
            log(Log.INFO, DeviceConfig.TAG, "BypassHook installed");
        } catch (Throwable e) {
            log(Log.ERROR, DeviceConfig.TAG, "BypassHook failed: " + e.getMessage());
        }

        try {
            DeviceInfoHook.INSTANCE.hook(this, cl);
            log(Log.INFO, DeviceConfig.TAG, "DeviceInfoHook installed");
        } catch (Throwable e) {
            log(Log.ERROR, DeviceConfig.TAG, "DeviceInfoHook failed: " + e.getMessage());
        }

        try {
            AuthHook.INSTANCE.hook(this, cl);
            log(Log.INFO, DeviceConfig.TAG, "AuthHook installed");
        } catch (Throwable e) {
            log(Log.ERROR, DeviceConfig.TAG, "AuthHook failed: " + e.getMessage());
        }

        try {
            BindHook.INSTANCE.hook(this, cl);
            log(Log.INFO, DeviceConfig.TAG, "BindHook installed");
        } catch (Throwable e) {
            log(Log.ERROR, DeviceConfig.TAG, "BindHook failed: " + e.getMessage());
        }

        try {
            TransportHook.INSTANCE.hook(this, cl);
            log(Log.INFO, DeviceConfig.TAG, "TransportHook installed");
        } catch (Throwable e) {
            log(Log.ERROR, DeviceConfig.TAG, "TransportHook failed: " + e.getMessage());
        }

        try {
            ScanHook.INSTANCE.hook(this, cl);
            log(Log.INFO, DeviceConfig.TAG, "ScanHook installed");
        } catch (Throwable e) {
            log(Log.ERROR, DeviceConfig.TAG, "ScanHook failed: " + e.getMessage());
        }

        try {
            WearOsHook.INSTANCE.hook(this, cl);
            log(Log.INFO, DeviceConfig.TAG, "WearOsHook installed");
        } catch (Throwable e) {
            log(Log.ERROR, DeviceConfig.TAG, "WearOsHook failed: " + e.getMessage());
        }

        try {
            DeviceListHook.INSTANCE.hook(this, cl);
            log(Log.INFO, DeviceConfig.TAG, "DeviceListHook installed");
        } catch (Throwable e) {
            log(Log.ERROR, DeviceConfig.TAG, "DeviceListHook failed: " + e.getMessage());
        }

        try {
            ProductHook.INSTANCE.hook(this, cl);
            log(Log.INFO, DeviceConfig.TAG, "ProductHook installed");
        } catch (Throwable e) {
            log(Log.ERROR, DeviceConfig.TAG, "ProductHook failed: " + e.getMessage());
        }

        try {
            SppAuthHook.INSTANCE.hook(this, cl);
            log(Log.INFO, DeviceConfig.TAG, "SppAuthHook installed");
        } catch (Throwable e) {
            log(Log.ERROR, DeviceConfig.TAG, "SppAuthHook failed: " + e.getMessage());
        }

        log(Log.INFO, DeviceConfig.TAG, "All mi.health hooks installed");
    }

    private void hookMiConnectService(ClassLoader cl) {
        log(Log.INFO, DeviceConfig.TAG, "Hooking com.xiaomi.mi_connect_service");

        try {
            BondHook.INSTANCE.hook(this, cl);
            log(Log.INFO, DeviceConfig.TAG, "BondHook installed for mi_connect_service");
        } catch (Throwable e) {
            log(Log.ERROR, DeviceConfig.TAG, "BondHook failed for mi_connect_service: " + e.getMessage());
        }

        try {
            SppAuthHook.INSTANCE.hook(this, cl);
            log(Log.INFO, DeviceConfig.TAG, "SppAuthHook installed for mi_connect_service");
        } catch (Throwable e) {
            log(Log.ERROR, DeviceConfig.TAG, "SppAuthHook failed for mi_connect_service: " + e.getMessage());
        }
    }

    private void hookBluetooth(String pkg, ClassLoader cl) {
        log(Log.INFO, DeviceConfig.TAG, "Hooking " + pkg);

        try {
            BluetoothHook.INSTANCE.hook(this, pkg, cl);
            log(Log.INFO, DeviceConfig.TAG, "BluetoothHook installed for " + pkg);
        } catch (Throwable e) {
            log(Log.ERROR, DeviceConfig.TAG, "BluetoothHook failed for " + pkg + ": " + e.getMessage());
        }
    }
}

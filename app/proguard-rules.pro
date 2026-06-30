-keep class io.github.pixelwatchspoof.HookEntry { *; }
-keep class io.github.pixelwatchspoof.hooks.** { *; }
-keep class io.github.pixelwatchspoof.config.** { *; }

-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

-keep class com.xiaomi.wearable.core.DeviceInfo { *; }
-keep class com.xiaomi.wearable.wear.api.WearAuthV2 { *; }
-keep class com.xiaomi.wearable.spp.SystemClient { *; }
-keep class com.xiaomi.wearable.NearbyService { *; }
-keep class com.xiaomi.wearable.transport.layerl2.L2Packet { *; }
-keep class com.xiaomi.fit.device.bind.BindToServer { *; }
-keep class com.xiaomi.fit.device.extensions.ProductExtKt { *; }

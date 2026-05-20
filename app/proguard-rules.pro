# CloudNG ProGuard rules

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# --- Data models (Gson / Room) ---
-keep class com.cloudng.app.data.model.** { *; }
-keepclassmembers class com.cloudng.app.data.model.** { *; }

# --- Room ---
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# --- Gson ---
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.** { *; }

# --- Hilt / Dagger ---
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class *
-keep @dagger.hilt.android.AndroidEntryPoint class *
-keep @dagger.Module class *
-keep @dagger.hilt.InstallIn class *

# --- Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# --- WorkManager ---
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# --- Core bridge (keep all public API) ---
-keep class com.cloudng.app.core.** { *; }

# --- Zxing QR ---
-keep class com.google.zxing.** { *; }
-keep class com.journeyapps.** { *; }

# --- Security crypto ---
-keep class androidx.security.crypto.** { *; }

# --- VpnService / Services / Receivers ---
-keep class com.cloudng.app.service.** { *; }
-keep class com.cloudng.app.receiver.** { *; }

# --- Suppress noisy warnings ---
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn java.lang.invoke.**
-dontwarn org.conscrypt.**

# --- Hilt annotation references (processed at compile-time, absent in release binary) ---
-dontwarn dagger.hilt.android.AndroidEntryPoint
-dontwarn dagger.hilt.android.HiltAndroidApp
-dontwarn dagger.hilt.android.components.ActivityComponent
-dontwarn dagger.hilt.android.components.ActivityRetainedComponent
-dontwarn dagger.hilt.android.components.ServiceComponent
-dontwarn dagger.hilt.android.components.ViewModelComponent
-dontwarn dagger.hilt.android.internal.OnReceiveBytecodeInjectionMarker
-dontwarn dagger.hilt.android.internal.managers.ActivityComponentManager
-dontwarn dagger.hilt.android.internal.managers.ApplicationComponentManager
-dontwarn dagger.hilt.android.internal.managers.BroadcastReceiverComponentManager
-dontwarn dagger.hilt.android.internal.managers.ComponentSupplier
-dontwarn dagger.hilt.android.internal.managers.ServiceComponentManager
-dontwarn dagger.hilt.android.lifecycle.HiltViewModel
-dontwarn dagger.hilt.android.qualifiers.ApplicationContext
-dontwarn dagger.hilt.internal.GeneratedEntryPoint
-dontwarn dagger.hilt.internal.aggregatedroot.AggregatedRoot
-dontwarn dagger.hilt.internal.processedrootsentinel.ProcessedRootSentinel
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# --- xrayNg native Go/libbox bindings (not on Maven Central, resolved at runtime) ---
-dontwarn com.tim.libbox.Libbox
-dontwarn com.tim.libbox.V2RayPoint
-dontwarn com.tim.libbox.V2RayVPNServiceSupportsSet
-dontwarn go.Seq
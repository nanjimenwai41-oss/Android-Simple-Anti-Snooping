# Xposed entry point
-keep public class com.antisnooping.hook.AntiSnoopingHook { *; }

# Xposed API
-keep public class de.robv.android.xposed.** { *; }
-dontwarn de.robv.android.xposed.**

# Keep all classes that implement Xposed interfaces
-keep public class * implements de.robv.android.xposed.IXposedHookLoadPackage
-keep public class * implements de.robv.android.xposed.IXposedHookInitPackageResources
-keep public class * implements de.robv.android.xposed.IXposedHookZygoteInit

# Kotlin specific
-keep class kotlin.Metadata { *; }

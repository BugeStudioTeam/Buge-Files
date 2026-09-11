-keep class rikka.shizuku.** { *; }
-keep class moe.shizuku.** { *; }
-keep class android.content.pm.IPackageManager { *; }
-keep class android.content.pm.IPackageManager$Stub { *; }
-keep class android.content.pm.IPackageInstallObserver { *; }
-keep class android.content.pm.IPackageDeleteObserver { *; }
-keepclassmembers class * {
    public void packageInstalled(java.lang.String, int);
    public void packageInstalled(java.lang.String, int, android.os.Bundle);
    public void packageDeleted(java.lang.String, int);
}
-keep class com.buge.files.ShizukuInstaller { *; }
-keep class com.buge.files.ShizukuManager { *; }
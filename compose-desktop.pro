# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# Rules needed for kotlinx.serialization
-if @kotlinx.serialization.Serializable class **
-keep class <1> {
    *;
}
# Rules needed for Room
-keep class * extends androidx.room.RoomDatabase
# Rules needed for SQLite
-keep class androidx.sqlite.driver.bundled.** { *; }
# Rules needed for Desktop
-keep class * implements kotlinx.coroutines.internal.MainDispatcherFactory
-keep class com.sun.jna.** { *; }
# Rules for filekit
-keep class * implements com.sun.jna.** { *; }
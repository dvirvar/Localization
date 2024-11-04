# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# Rules needed for kotlinx.serialization
-if @kotlinx.serialization.Serializable class **
-keep class <1> {
    *;
}
#-keepclasseswithmembers class **.*$Companion {
#    kotlinx.serialization.KSerializer serializer(...);
#}
## If a companion has the serializer function, keep the companion field on the original type so that
## the reflective lookup succeeds.
#-if class **.*$Companion {
#  kotlinx.serialization.KSerializer serializer(...);
#}
#-keepclassmembers class <1>.<2> {
#  <1>.<2>$Companion Companion;
#}
# Rules needed for Room
-keep class * extends androidx.room.RoomDatabase
# Rules needed for SQLite
-keep class androidx.sqlite.driver.bundled.**
# Rules needed for Desktop
-keep class * implements kotlinx.coroutines.internal.MainDispatcherFactory
-keep class com.sun.jna.** { *; }
# Rules needed for compose FOR NOW!!!
-keep class androidx.compose.foundation.** { *; }
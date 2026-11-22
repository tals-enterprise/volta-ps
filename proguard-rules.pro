# The native bridge (src/main/cpp/ps-jni.c) binds PsNative's methods by their
# JNI names and calls the session listener's methods by name from chiaki's
# threads: neither may be renamed or dropped.
-keep class com.tals.volta.play.PsNative { *; }
-keep interface com.tals.volta.play.PsNative$Listener { *; }
-keep class * implements com.tals.volta.play.PsNative$Listener { *; }
-keepattributes SourceFile,LineNumberTable

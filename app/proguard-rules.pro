# Release shrinking (R8). Libraries used here ship their own rules (Compose, Room, Navigation,
# kotlinx.serialization, CameraX); these keep our own code readable in crash logs.

# Line numbers in stack traces (the log's app.crash line).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Type-safe navigation routes are found through their serializers.
-keep @kotlinx.serialization.Serializable class fi.jukkakot.rubikkisolveri.ui.nav.** { *; }

# R8 rules for Shushop.
# Room, Compose, Coroutines and Navigation ship their own rules inside their libraries.
# Our data classes are kept whole so database entities and JSON parsing stay readable in crash logs.
-keep class uz.shushop.app.data.** { *; }
-keepattributes SourceFile,LineNumberTable

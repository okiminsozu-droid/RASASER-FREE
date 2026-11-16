# Release signing must remain PRIVATE.

# Valhalla Mobile's JNI bridge refers to the original fully-qualified class
# and method names. R8 must not rename/remove these native method bindings.
-keep class com.valhalla.valhalla.** { *; }

# The mobile Valhalla actor serializes generated Kotlin request, response and
# configuration classes reflectively using Moshi KotlinJsonAdapterFactory.
# These fields must retain their modeled names in optimized field-test/release.
-keep class com.valhalla.api.models.** { *; }
-keep class com.valhalla.config.models.** { *; }
-keep class com.osrm.api.models.** { *; }

# Java compiler-only annotation type referenced by AndroidX instrumentation
# when the test APK is minified. It is not a runtime Android class.
-dontwarn javax.lang.model.element.Modifier

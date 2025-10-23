# Add project specific ProGuard rules here.
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

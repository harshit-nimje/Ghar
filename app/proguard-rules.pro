# Add project specific ProGuard / R8 rules here.
# Keep line numbers for actionable production crash stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve Kotlin metadata and signature attributes for reflection / Firebase / Coroutines
-keepattributes Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations

# Preserve all domain models & enums serialized to/from Firebase Firestore or JSON
-keep class com.example.core.model.** { *; }
-keepclassmembers enum com.example.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Preserve Firebase Firestore POJO / DocumentSnapshot mappers & AI Logic SDK types
-keep class com.google.firebase.firestore.** { *; }
-keep class com.google.firebase.ai.** { *; }
-dontwarn com.google.firebase.**

# Preserve Jetpack Credential Manager & Google Identity Services entry points
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# Untuk WebView
-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String, android.graphics.Bitmap);
    public boolean *(android.webkit.WebView, java.lang.String);
}

# Untuk JavaScript interface
-keepattributes *Annotation*
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Untuk Service
-keep public class * extends android.app.Service

# Untuk proses manager
-keep class com.gamebooster.real.ProcessManager { *; }

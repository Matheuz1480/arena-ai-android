# Regras específicas do app Arena AI
# (WebView wrapper — sem reflection, sem regras extras necessárias)
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

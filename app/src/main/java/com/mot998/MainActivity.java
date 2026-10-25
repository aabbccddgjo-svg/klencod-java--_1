package com.mot998;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebStorage;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;

public class MainActivity extends Activity {

    private WebView webView;
    private EditText urlInput;
    private ProgressBar progressBar;
    private TextView adsBlockedTextView;
    private int blockedAdsCount = 0;

    private static final String[] AD_FILTERS = {
            "doubleclick.net",
            "googleadservices.com",
            "googlesyndication.com",
            "adnxs.com",
            "facebook.com/tr/",
            "analytics.google.com",
            "adservice.google.com"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Color.parseColor("#121212"));

        // شريط العنوان والأدوات العلوي
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setPadding(20, 15, 20, 15);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setBackgroundColor(Color.parseColor("#1F1F1F"));

        urlInput = new EditText(this);
        urlInput.setHint("ابحث أو أدخل رابط الموقع...");
        urlInput.setHintTextColor(Color.GRAY);
        urlInput.setTextColor(Color.WHITE);
        urlInput.setSingleLine(true);
        urlInput.setTextSize(14);
        urlInput.setPadding(30, 20, 30, 20);
        
        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setColor(Color.parseColor("#2C2C2C"));
        inputBg.setCornerRadius(25);
        urlInput.setBackground(inputBg);

        LinearLayout.LayoutParams urlParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        urlParams.setMargins(0, 0, 10, 0);
        urlInput.setLayoutParams(urlParams);
        topBar.addView(urlInput);

        // زر الانتقال
        Button goButton = createStyledButton("ذهاب", Color.parseColor("#FF5722"));
        goButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadUrlFromInput();
            }
        });
        topBar.addView(goButton);

        // زر مسح بيانات الموقع الحالي (ميزة Brave)
        Button clearSiteDataBtn = createStyledButton("🧹 مسح البيانات", Color.parseColor("#D32F2F"));
        clearSiteDataBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearCurrentSiteData();
            }
        });
        topBar.addView(clearSiteDataBtn);

        mainLayout.addView(topBar);

        // شريط درع الخصوصية
        LinearLayout shieldBar = new LinearLayout(this);
        shieldBar.setOrientation(LinearLayout.HORIZONTAL);
        shieldBar.setPadding(25, 10, 25, 10);
        shieldBar.setBackgroundColor(Color.parseColor("#292929"));

        adsBlockedTextView = new TextView(this);
        adsBlockedTextView.setText("🛡️ درع الخصوصية: 0 إعلانات ومتتبع محظور");
        adsBlockedTextView.setTextColor(Color.parseColor("#4CAF50"));
        adsBlockedTextView.setTextSize(12);
        shieldBar.addView(adsBlockedTextView);

        mainLayout.addView(shieldBar);

        // شريط التحميل
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setVisibility(View.GONE);
        mainLayout.addView(progressBar);

        // محرك المتصفح
        webView = new WebView(this);
        LinearLayout.LayoutParams webParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        webView.setLayoutParams(webParams);
        mainLayout.addView(webView);

        // شريط التنقل السفلي
        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setPadding(10, 10, 10, 10);
        bottomBar.setGravity(Gravity.CENTER);
        bottomBar.setBackgroundColor(Color.parseColor("#1F1F1F"));

        Button backBtn = createStyledButton("◀ خلف", Color.parseColor("#333333"));
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (webView.canGoBack()) webView.goBack();
            }
        });

        Button forwardBtn = createStyledButton("أمام ▶", Color.parseColor("#333333"));
        forwardBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (webView.canGoForward()) webView.goForward();
            }
        });

        Button refreshBtn = createStyledButton("🔄 تحديث", Color.parseColor("#333333"));
        refreshBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.reload();
            }
        });

        bottomBar.addView(backBtn);
        bottomBar.addView(forwardBtn);
        bottomBar.addView(refreshBtn);
        mainLayout.addView(bottomBar);

        setContentView(mainLayout);

        setupBravePrivacySettings();
        webView.loadUrl("https://duckduckgo.com");
    }

    private void setupBravePrivacySettings() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setSavePassword(false);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, false);

        webView.setWebViewClient(new SecureWebViewClient());
    }

    // دالة مسح بيانات الموقع الحالي
    private void clearCurrentSiteData() {
        String currentUrl = webView.getUrl();
        if (currentUrl == null || currentUrl.isEmpty()) {
            Toast.makeText(this, "لا يوجد موقع مفتوح حالياً لمسح بياناته", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Uri uri = Uri.parse(currentUrl);
            String host = uri.getHost();

            if (host != null) {
                // 1. مسح الـ Web Storage والتخزين المحلي المخصص لنطاق الموقع
                WebStorage.getInstance().deleteOrigin(uri.getScheme() + "://" + host);

                // 2. مسح ملفات تعريف الارتباط (Cookies) الخاصة بالموقع
                CookieManager cookieManager = CookieManager.getInstance();
                String cookies = cookieManager.getCookie(currentUrl);
                if (cookies != null) {
                    String[] cookieArray = cookies.split(";");
                    for (String cookie : cookieArray) {
                        String[] cookieNameValue = cookie.split("=");
                        if (cookieNameValue.length > 0) {
                            cookieManager.setCookie(currentUrl, cookieNameValue[0].trim() + "=; Expires=Thu, 01 Jan 1970 00:00:00 GMT");
                        }
                    }
                    cookieManager.flush();
                }

                // 3. تنظيف التخزين المؤقت (Cache) وإعادة تحميل الصفحة
                webView.clearCache(true);
                webView.reload();

                Toast.makeText(this, "تم مسح كافة بيانات الموقع: " + host, Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "حدث خطأ أثناء مسح البيانات: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadUrlFromInput() {
        String url = urlInput.getText().toString().trim();
        if (url.isEmpty()) return;

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            if (url.contains(".") && !url.contains(" ")) {
                url = "https://" + url;
            } else {
                url = "https://duckduckgo.com/?q=" + url;
            }
        }
        webView.loadUrl(url);
    }

    private class SecureWebViewClient extends WebViewClient {

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            progressBar.setVisibility(View.VISIBLE);
            urlInput.setText(url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            progressBar.setVisibility(View.GONE);
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString().toLowerCase();

            for (String adDomain : AD_FILTERS) {
                if (url.contains(adDomain)) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            blockedAdsCount++;
                            adsBlockedTextView.setText("🛡️ درع الخصوصية: " + blockedAdsCount + " إعلانات ومتتبع محظور");
                        }
                    });
                    return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream("".getBytes()));
                }
            }
            return super.shouldInterceptRequest(view, request);
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private Button createStyledButton(String text, int bgColor) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(11);

        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(15);
        shape.setColor(bgColor);
        button.setBackground(shape);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(4, 0, 4, 0);
        button.setLayoutParams(params);

        return button;
    }
}

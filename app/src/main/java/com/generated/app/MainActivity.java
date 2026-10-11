package com.generated.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final String TARGET_URL = "https://app-factory-nt5l.onrender.com/";
    private static final int PERMISSION_REQ_NOTIFICATIONS = 101;

    private WebView webView;
    private ProgressBar progressBar;
    private View errorView;
    private Button btnToggleBackground;
    private boolean isBackgroundServiceRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupWebView();
        requestNotificationPermissionIfNeeded();
        startBackgroundService();
        loadTargetUrl();
    }

    private void initViews() {
        webView = (WebView) findViewById(R.id.webView);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        errorView = (errorView = findViewById(R.id.errorView));
        btnToggleBackground = (Button) findViewById(R.id.btnToggleBackground);
        ImageButton btnRefresh = (ImageButton) findViewById(R.id.btnRefresh);
        Button btnRetry = (Button) findViewById(R.id.btnRetry);

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                errorView.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
                webView.reload();
            }
        });

        btnRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                errorView.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
                loadTargetUrl();
            }
        });

        btnToggleBackground.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleBackgroundService();
            }
        });
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
                errorView.setVisibility(View.GONE);
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                super.onPageFinished(view, url);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                showErrorScreen();
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
                super.onProgressChanged(view, newProgress);
            }
        });
    }

    private void loadTargetUrl() {
        if (!isNetworkConnected()) {
            showErrorScreen();
            return;
        }
        errorView.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
        webView.loadUrl(TARGET_URL);
    }

    private void showErrorScreen() {
        progressBar.setVisibility(View.GONE);
        webView.setVisibility(View.GONE);
        errorView.setVisibility(View.VISIBLE);
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, PERMISSION_REQ_NOTIFICATIONS);
            }
        }
    }

    private void startBackgroundService() {
        try {
            Intent serviceIntent = new Intent(this, BackgroundMonitorService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            isBackgroundServiceRunning = true;
            updateBgButtonUi(true);
        } catch (Exception e) {
            updateBgButtonUi(false);
        }
    }

    private void stopBackgroundService() {
        Intent serviceIntent = new Intent(this, BackgroundMonitorService.class);
        stopService(serviceIntent);
        isBackgroundServiceRunning = false;
        updateBgButtonUi(false);
    }

    private void toggleBackgroundService() {
        if (isBackgroundServiceRunning) {
            stopBackgroundService();
            Toast.makeText(this, "שירות הרקע הופסק", Toast.LENGTH_SHORT).show();
        } else {
            startBackgroundService();
            Toast.makeText(this, "שירות הרקע וההתראות הופעל", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateBgButtonUi(boolean active) {
        if (active) {
            btnToggleBackground.setText(R.string.bg_active);
            btnToggleBackground.setTextColor(0xFF00E676);
        } else {
            btnToggleBackground.setText(R.string.bg_inactive);
            btnToggleBackground.setTextColor(0xFFFF5252);
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}

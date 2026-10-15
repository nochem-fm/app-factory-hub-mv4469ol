package com.generated.app;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Base64;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;

public class MainActivity extends Activity {

    public static final String PACKAGE_IDENTIFIER = "com.app.factory.fm";
    public static final String TARGET_URL = "https://app-factory-nt5l.onrender.com/";
    private static final String INTERNAL_HOST = "app-factory-nt5l.onrender.com";

    private static final int PERMISSION_REQ_NOTIFICATIONS = 101;
    private static final int PERMISSION_REQ_STORAGE = 102;
    private static final int REQ_INSTALL_UNKNOWN_APPS = 103;
    private static final int FILE_CHOOSER_RESULT_CODE = 2001;

    private WebView webView;
    private ProgressBar progressBar;
    private View errorView;
    private Button btnToggleBackground;
    private boolean isBackgroundServiceRunning = false;
    private boolean isReceiverRegistered = false;

    private ValueCallback<Uri[]> uploadMessage;
    private ValueCallback<Uri> uploadMessageAboveL;

    private File pendingInstallApk = null;
    private String pendingDownloadUrl = null;
    private String pendingUserAgent = null;
    private String pendingContentDisposition = null;
    private String pendingMimetype = null;

    private final BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (downloadId != -1) {
                    handleDownloadCompleted(downloadId);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupWebView();
        requestNotificationPermissionIfNeeded();
        startBackgroundService();

        registerDownloadReceiverSafe();
        loadTargetUrl();
    }

    private void registerDownloadReceiverSafe() {
        try {
            IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(downloadReceiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                registerReceiver(downloadReceiver, filter);
            }
            isReceiverRegistered = true;
        } catch (Exception e) {
            try {
                registerReceiver(downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
                isReceiverRegistered = true;
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(downloadReceiver);
                isReceiverRegistered = false;
            } catch (Exception ignored) {
            }
        }
        super.onDestroy();
    }

    private void initViews() {
        webView = (WebView) findViewById(R.id.webView);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        errorView = findViewById(R.id.errorView);
        btnToggleBackground = (Button) findViewById(R.id.btnToggleBackground);
        ImageButton btnRefresh = (ImageButton) findViewById(R.id.btnRefresh);
        ImageButton btnAbout = (ImageButton) findViewById(R.id.btnAbout);
        ImageButton btnDownloads = (ImageButton) findViewById(R.id.btnDownloads);
        Button btnRetry = (Button) findViewById(R.id.btnRetry);

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                errorView.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
                webView.reload();
            }
        });

        btnAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AboutActivity.class);
                startActivity(intent);
            }
        });

        if (btnDownloads != null) {
            btnDownloads.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDownloadsListDialog();
                }
            });
        }

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
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }

        // Bridge for handling blob downloads and direct client APKs & Web Push Notifications
        webView.addJavascriptInterface(new AppFactoryJsBridge(), "AppFactoryBridge");
        webView.addJavascriptInterface(new BlobDownloadInterface(), "AndroidDownloader");

        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                processDownload(url, userAgent, contentDisposition, mimetype);
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrlNavigation(url);
            }

            @TargetApi(Build.VERSION_CODES.N)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (request != null && request.getUrl() != null) {
                    return handleUrlNavigation(request.getUrl().toString());
                }
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
                errorView.setVisibility(View.GONE);
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                // Inject helper script to bridge Service Worker / Web Notifications & Multi-file Uploads if needed
                injectWebBridgeScript();
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

            // For Android 4.1+
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (uploadMessage != null) {
                    uploadMessage.onReceiveValue(null);
                    uploadMessage = null;
                }

                uploadMessage = filePathCallback;

                Intent intent = null;
                if (fileChooserParams != null) {
                    try {
                        intent = fileChooserParams.createIntent();
                        // Enable multiple file selection if requested by website
                        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    } catch (Exception ignored) {
                    }
                }

                if (intent == null) {
                    intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("*/*");
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                }

                try {
                    startActivityForResult(Intent.createChooser(intent, "בחר קבצים להעלאה"), FILE_CHOOSER_RESULT_CODE);
                } catch (Exception e) {
                    uploadMessage = null;
                    Toast.makeText(MainActivity.this, "לא ניתן לפתוח בוחר קבצים", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            }
        });
    }

    private void injectWebBridgeScript() {
        String script = "javascript:(function() {" +
                "  if (window.Notification && window.Notification.requestPermission) {" +
                "    window.Notification.requestPermission = function(callback) {" +
                "      if (callback) callback('granted');" +
                "      return Promise.resolve('granted');" +
                "    };" +
                "  }" +
                "  const originalShowNotification = window.Notification;" +
                "  if (window.ServiceWorkerRegistration && window.ServiceWorkerRegistration.prototype) {" +
                "    window.ServiceWorkerRegistration.prototype.showNotification = function(title, options) {" +
                "      let body = options ? options.body : '';" +
                "      AppFactoryBridge.showNotification(title, body);" +
                "    };" +
                "  }" +
                "})();";
        webView.loadUrl(script);
    }

    public class AppFactoryJsBridge {
        @JavascriptInterface
        public void showNotification(final String title, final String body) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    BackgroundMonitorService.showDynamicNotification(MainActivity.this, title, body);
                }
            });
        }
    }

    private boolean handleUrlNavigation(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        String lowerUrl = url.toLowerCase();
        if (lowerUrl.endsWith(".apk") || lowerUrl.contains(".apk?")) {
            processDownload(url, webView.getSettings().getUserAgentString(), null, "application/vnd.android.package-archive");
            return true;
        }

        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme();

        if (scheme != null && !scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) {
            return handleExternalLink(url);
        }

        String host = uri.getHost();
        if (host != null && host.equalsIgnoreCase(INTERNAL_HOST)) {
            return false; // Load inside WebView
        }

        return handleExternalLink(url);
    }

    public void processDownload(String url, String userAgent, String contentDisposition, String mimetype) {
        if (url == null) return;

        if (url.startsWith("blob:")) {
            handleBlobUrlDownload(url);
            return;
        }

        if (url.startsWith("data:")) {
            handleDataUriDownload(url);
            return;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                pendingDownloadUrl = url;
                pendingUserAgent = userAgent;
                pendingContentDisposition = contentDisposition;
                pendingMimetype = mimetype;
                requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQ_STORAGE);
                return;
            }
        }

        enqueueSystemDownload(url, userAgent, contentDisposition, mimetype);
    }

    private void enqueueSystemDownload(String url, String userAgent, String contentDisposition, String mimetype) {
        try {
            String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);
            if (fileName == null || fileName.isEmpty() || fileName.toLowerCase().endsWith(".bin")) {
                String extension = getExtensionForMimeType(mimetype);
                fileName = "download-" + System.currentTimeMillis() + "." + extension;
            }

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            if (mimetype != null && !mimetype.isEmpty()) {
                request.setMimeType(mimetype);
            }

            String cookies = CookieManager.getInstance().getCookie(url);
            if (cookies != null) {
                request.addRequestHeader("cookie", cookies);
            }
            if (userAgent != null) {
                request.addRequestHeader("User-Agent", userAgent);
            }

            request.setTitle(fileName);
            request.setDescription(getString(R.string.downloading_file) + ": " + fileName);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

            DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm != null) {
                dm.enqueue(request);
                Toast.makeText(this, getString(R.string.download_started) + " " + fileName, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.download_failed), Toast.LENGTH_SHORT).show();
        }
    }

    private void handleBlobUrlDownload(String blobUrl) {
        String js = "javascript:(function(){"
                + "  var xhr = new XMLHttpRequest();"
                + "  xhr.open('GET', '" + blobUrl + "', true);"
                + "  xhr.responseType = 'blob';"
                + "  xhr.onload = function() {"
                + "    var reader = new FileReader();"
                + "    reader.onloadend = function() {"
                + "      AndroidDownloader.saveBase64(reader.result);"
                + "    };"
                + "    reader.readAsDataURL(xhr.response);"
                + "  };"
                + "  xhr.send();"
                + "})()";
        webView.loadUrl(js);
    }

    private void handleDataUriDownload(String dataUri) {
        try {
            int commaIndex = dataUri.indexOf(',');
            if (commaIndex > 0) {
                String base64Data = dataUri.substring(commaIndex + 1);
                String mimeTypePart = dataUri.substring(0, commaIndex);
                String mimeType = "application/octet-stream";

                int mimeTypeStart = mimeTypePart.indexOf(':');
                int mimeTypeEnd = mimeTypePart.indexOf(';');

                if (mimeTypeStart != -1 && mimeTypeEnd != -1 && mimeTypeStart < mimeTypeEnd) {
                    mimeType = mimeTypePart.substring(mimeTypeStart + 1, mimeTypeEnd);
                } else if (mimeTypeStart != -1) {
                    mimeType = mimeTypePart.substring(mimeTypeStart + 1);
                }

                byte[] bytes = Base64.decode(base64Data, Base64.DEFAULT);

                String extension = getExtensionForMimeType(mimeType);
                final String finalFilename = "file-" + System.currentTimeMillis() + "." + extension;

                saveFileToDownloads(bytes, finalFilename, mimeType);
            }
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.download_failed), Toast.LENGTH_SHORT).show();
        }
    }

    private class BlobDownloadInterface {
        @JavascriptInterface
        public void saveBase64(final String dataUri) {
            if (dataUri == null || !dataUri.startsWith("data:")) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, getString(R.string.download_failed), Toast.LENGTH_SHORT).show();
                    }
                });
                return;
            }

            int commaIndex = dataUri.indexOf(',');
            if (commaIndex <= 0) {
                 runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, getString(R.string.download_failed), Toast.LENGTH_SHORT).show();
                    }
                });
                return;
            }

            final String base64Data = dataUri.substring(commaIndex + 1);
            String mimeTypePart = dataUri.substring(0, commaIndex);
            String mimeType = "application/octet-stream";

            int mimeTypeStart = mimeTypePart.indexOf(':');
            int mimeTypeEnd = mimeTypePart.indexOf(';');

            if (mimeTypeStart != -1 && mimeTypeEnd != -1 && mimeTypeStart < mimeTypeEnd) {
                mimeType = mimeTypePart.substring(mimeTypeStart + 1, mimeTypeEnd);
            } else if (mimeTypeStart != -1) {
                 mimeType = mimeTypePart.substring(mimeTypeStart + 1);
            }

            final String actualMimeType = mimeType;

            String extension = getExtensionForMimeType(actualMimeType);
            final String finalFilename = "download-" + System.currentTimeMillis() + "." + extension;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        byte[] decodedBytes = Base64.decode(base64Data, Base64.DEFAULT);
                        saveFileToDownloads(decodedBytes, finalFilename, actualMimeType);
                    } catch (Exception e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, getString(R.string.download_failed), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }).start();
        }
    }

    private void saveFileToDownloads(final byte[] data, final String filename, final String mimeType) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    if (!downloadDir.exists()) {
                        downloadDir.mkdirs();
                    }
                    final File savedFile = new File(downloadDir, filename);
                    FileOutputStream fos = new FileOutputStream(savedFile);
                    fos.write(data);
                    fos.flush();
                    fos.close();

                    // Add to DownloadManager database so it shows up in system downloads list / notifications
                    try {
                        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                        if (dm != null) {
                            dm.addCompletedDownload(filename, "App Factory Download", true, mimeType, savedFile.getAbsolutePath(), data.length, true);
                        }
                    } catch (Exception ignored) {
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, getString(R.string.download_finished) + ": " + filename, Toast.LENGTH_SHORT).show();
                            if (isApkMimeType(mimeType)) {
                                promptInstallApk(savedFile);
                            }
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, getString(R.string.download_failed), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void handleDownloadCompleted(long downloadId) {
        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        if (dm == null) return;

        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);
        Cursor cursor = null;
        try {
            cursor = dm.query(query);
            if (cursor != null && cursor.moveToFirst()) {
                int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                if (statusIndex >= 0 && cursor.getInt(statusIndex) == DownloadManager.STATUS_SUCCESSFUL) {
                    int localUriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                    int titleIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE);
                    int mediaTypeIndex = cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE);

                    String localUriStr = localUriIndex >= 0 ? cursor.getString(localUriIndex) : null;
                    String title = titleIndex >= 0 ? cursor.getString(titleIndex) : "";
                    String mediaType = mediaTypeIndex >= 0 ? cursor.getString(mediaTypeIndex) : null;

                    if (localUriStr != null) {
                        Uri uri = Uri.parse(localUriStr);
                        File file = new File(uri.getPath());

                        if (mediaType == null && file.exists()) {
                            mediaType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(file.getAbsolutePath()));
                            if (mediaType == null) {
                                if (file.getName().toLowerCase().endsWith(".apk")) {
                                    mediaType = "application/vnd.android.package-archive";
                                }
                            }
                        }

                        if (file.exists() && isApkMimeType(mediaType)) {
                            promptInstallApk(file);
                        } else {
                            Toast.makeText(this, getString(R.string.download_finished) + ": " + title, Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void promptInstallApk(final File apkFile) {
        if (apkFile == null || !apkFile.exists()) {
            Toast.makeText(this, getString(R.string.file_not_found), Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.install_app_dialog_title);
        builder.setMessage(getString(R.string.install_app_dialog_msg, apkFile.getName()));
        builder.setIcon(R.drawable.ic_launcher);
        builder.setPositiveButton(R.string.install_now, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                installApkFile(apkFile);
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    public void installApkFile(File apkFile) {
        if (apkFile == null || !apkFile.exists()) {
            Toast.makeText(this, getString(R.string.file_not_found), Toast.LENGTH_SHORT).show();
            return;
        }

        pendingInstallApk = apkFile;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            boolean canInstall = getPackageManager().canRequestPackageInstalls();
            if (!canInstall) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.permission_unknown_apps_title);
                builder.setMessage(R.string.permission_unknown_apps_msg);
                builder.setPositiveButton(R.string.grant_permission, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            startActivityForResult(intent, REQ_INSTALL_UNKNOWN_APPS);
                        } catch (Exception e) {
                            try {
                                Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                                startActivityForResult(intent, REQ_INSTALL_UNKNOWN_APPS);
                            } catch (Exception ex) {
                                Toast.makeText(MainActivity.this, getString(R.string.cannot_open_settings), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
                builder.setNegativeButton(R.string.cancel, null);
                builder.show();
                return;
            }
        }

        launchInstallerIntent(apkFile);
    }

    private void launchInstallerIntent(File apkFile) {
        try {
            Uri apkUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                apkUri = AppFileProvider.getUriForFile(this, apkFile);
            } else {
                apkUri = Uri.fromFile(apkFile);
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.install_failed_intent) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_INSTALL_UNKNOWN_APPS) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (getPackageManager().canRequestPackageInstalls()) {
                    if (pendingInstallApk != null && pendingInstallApk.exists()) {
                        launchInstallerIntent(pendingInstallApk);
                        pendingInstallApk = null;
                    }
                } else {
                    Toast.makeText(this, getString(R.string.permission_denied_install), Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (uploadMessage == null) return;

            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null) {
                String dataString = data.getDataString();
                if (dataString != null) {
                    results = new Uri[]{Uri.parse(dataString)};
                } else if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    results = new Uri[count];
                    for (int i = 0; i < count; i++) {
                        results[i] = data.getClipData().getItemAt(i).getUri();
                    }
                }
            }

            uploadMessage.onReceiveValue(results);
            uploadMessage = null;
        }
    }

    private void showDownloadsListDialog() {
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!downloadDir.exists()) {
            Toast.makeText(this, getString(R.string.no_downloaded_apks), Toast.LENGTH_SHORT).show();
            return;
        }

        File[] files = downloadDir.listFiles();
        if (files == null || files.length == 0) {
            Toast.makeText(this, getString(R.string.no_downloaded_apks), Toast.LENGTH_SHORT).show();
            return;
        }

        java.util.ArrayList<File> apkList = new java.util.ArrayList<File>();
        for (File f : files) {
            if (f.isFile() && f.getName().toLowerCase().endsWith(".apk")) {
                apkList.add(f);
            }
        }

        if (apkList.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_downloaded_apks), Toast.LENGTH_SHORT).show();
            return;
        }

        final File[] apks = apkList.toArray(new File[0]);
        String[] names = new String[apks.length];
        for (int i = 0; i < apks.length; i++) {
            long sizeMb = apks[i].length() / (1024 * 1024);
            names[i] = apks[i].getName() + (sizeMb > 0 ? " (" + sizeMb + " MB)" : "");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.downloaded_apks_title);
        builder.setItems(names, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                promptInstallApk(apks[which]);
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private boolean handleExternalLink(String url) {
        if (url == null) return false;
        try {
            if (url.startsWith("intent:")) {
                Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                if (intent != null) {
                    PackageManager pm = getPackageManager();
                    if (intent.resolveActivity(pm) != null) {
                        startActivity(intent);
                        return true;
                    }
                    String fallbackUrl = intent.getStringExtra("browser_fallback_url");
                    if (fallbackUrl != null) {
                        Intent fallbackIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl));
                        fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(fallbackIntent);
                        return true;
                    }
                }
                return true;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.no_app_to_open_link), Toast.LENGTH_SHORT).show();
            return true;
        }
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (pendingDownloadUrl != null) {
                    enqueueSystemDownload(pendingDownloadUrl, pendingUserAgent, pendingContentDisposition, pendingMimetype);
                    pendingDownloadUrl = null;
                }
            } else {
                Toast.makeText(this, getString(R.string.storage_permission_required), Toast.LENGTH_SHORT).show();
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
            isBackgroundServiceRunning = false;
            updateBgButtonUi(false);
        }
    }

    private void stopBackgroundService() {
        try {
            Intent serviceIntent = new Intent(this, BackgroundMonitorService.class);
            stopService(serviceIntent);
            isBackgroundServiceRunning = false;
            updateBgButtonUi(false);
        } catch (Exception ignored) {
        }
    }

    private void toggleBackgroundService() {
        if (isBackgroundServiceRunning) {
            stopBackgroundService();
            Toast.makeText(this, getString(R.string.bg_service_stopped), Toast.LENGTH_SHORT).show();
        } else {
            startBackgroundService();
            Toast.makeText(this, getString(R.string.bg_service_started), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateBgButtonUi(boolean active) {
        if (btnToggleBackground == null) return;
        if (active) {
            btnToggleBackground.setText(R.string.bg_active);
            btnToggleBackground.setTextColor(0xFF00E676);
        } else {
            btnToggleBackground.setText(R.string.bg_inactive);
            btnToggleBackground.setTextColor(0xFFFF5252);
        }
    }

    private boolean isApkMimeType(String mimeType) {
        return mimeType != null && "application/vnd.android.package-archive".equalsIgnoreCase(mimeType);
    }

    private String getExtensionForMimeType(String mimeType) {
        if (isApkMimeType(mimeType)) {
            return "apk";
        }
        String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        return (extension != null && !extension.isEmpty()) ? extension : "bin";
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

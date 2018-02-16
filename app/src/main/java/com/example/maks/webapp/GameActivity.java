package com.example.maks.webapp;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v13.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.webkit.CookieSyncManager;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.example.maks.webapp.db.FirebaseDB;
import com.example.maks.webapp.listeners.OnForbiddenFileExtensionChangeListener;

import java.util.ArrayList;
import java.util.List;

public class GameActivity extends BaseActivity implements OnForbiddenFileExtensionChangeListener {

    private final static int FILE_CHOOSER_RESULT_CODE = 1;
    private final static int REQUEST_CODE = 2;

    private String url;
    private String mimeType;
    private String contentDescription;
    private String userAgent;

    private WebView webView;
    private ProgressDialog progressDialog;
    private Boolean exit = false;
    private ValueCallback<Uri[]> mUploadMessage;

    private List<String> forbiddenFileExtension;
    private List<String> rules;

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        forbiddenFileExtension = new ArrayList<>();
        new FirebaseDB().setOnForbiddenFileExtensionChangeListener(this);

        rules = getIntent().getStringArrayListExtra(SplashActivity.RULES);
        if(rules == null)
            rules = new ArrayList<>();

        progressDialog = new ProgressDialog(this, ProgressDialog.THEME_HOLO_LIGHT);
        progressDialog.setMessage("Page loading!");

        webView = (WebView) findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(false);
        webView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.getSettings().setGeolocationEnabled(true);
        webView.getSettings().setAppCacheEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setAllowFileAccessFromFileURLs(true);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        webView.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);

        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        else webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        webView.clearCache(true);

        Intent intent = getIntent();
        if (intent.getIntExtra(SplashActivity.LOAD_TYPE, SplashActivity.LOAD_FROM_CACHE) != SplashActivity.LOAD_REFERENCE) {
            webView.loadUrl(intent.getStringExtra(SplashActivity.GAME_LINK));
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView webView, String url) {
                    webView.loadUrl(url);
                    return false;
                }
            });

        } else {
            progressDialog.show();
            webView.loadUrl(intent.getStringExtra(SplashActivity.REFERENCE));
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    if(url.equals(intent.getStringExtra(SplashActivity.INTERNAL_REFERENCES))) {
                        view.loadUrl(intent.getStringExtra(SplashActivity.GAME_LINK));

                        if(progressDialog.isShowing())
                            progressDialog.dismiss();

                    } else {
                        view.loadUrl(url);
                    }
                    return false;
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    CookieSyncManager.getInstance().sync();
                    for(String rule : rules) {
                        view.loadUrl("javascript:var x = document.querySelectorAll('" + rule + "')[0].style.display = 'none';");
                    }
                }
            });

            webView.setWebChromeClient(new WebChromeClient() {
                @Override
                public void onProgressChanged(WebView webView, int progress) {
                    if(progress < 100 && !progressDialog.isShowing())
                        progressDialog.show();

                    if(progress == 100)
                        progressDialog.dismiss();
                }

                @Override
                public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                    mUploadMessage = filePathCallback;

                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("*/*");
                    startActivityForResult(intent, FILE_CHOOSER_RESULT_CODE);

                    return true;
                }

            });

            webView.setDownloadListener((url, userAgent, contentDescription, mimeType, contentLength) -> {

                if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE);
                    this.url = url;
                    this.userAgent = userAgent;
                    this.contentDescription = contentDescription;
                    this.mimeType = mimeType;

                } else downloadFile(url, mimeType, userAgent, contentDescription);

            });

        }
    }

    @Override
    public void onResume() {
        super.onResume();
        CookieSyncManager.getInstance().startSync();
    }

    @Override
    public void onPause() {
        super.onPause();
        CookieSyncManager.getInstance().stopSync();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {

        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (null == mUploadMessage)
                return;

            Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();
            Uri[] resultsArray = new Uri[1];
            resultsArray[0] = result;

            if(resultsArray[0] != null) {
                mUploadMessage.onReceiveValue(resultsArray);
                mUploadMessage = null;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode){
            case REQUEST_CODE:{
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    downloadFile(url, mimeType, userAgent, contentDescription);
                }else {
                    Toast.makeText(this, "You don't have needed permission for download file", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else if (exit) {
            finish();
        } else {
            exit = true;
            Toast.makeText(this, "Press Back again to Exit.", Toast.LENGTH_SHORT).show();
            new Handler().postDelayed(() -> exit = false, 3 * 1000);
        }
    }

    private void downloadFile(String url, String mimeType, String userAgent, String contentDescription) {
        Log.e(getClass().getName(), "Download started");

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        String fileName = URLUtil.guessFileName(url, contentDescription, mimeType);
        String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1);

        if(forbiddenFileExtension.contains(fileExtension)) {
            Toast.makeText(this, "App doesn't support this file type", Toast.LENGTH_SHORT).show();
            return;
        }

        request.setMimeType(mimeType);
        request.addRequestHeader("User-Agent", userAgent);
        request.setDescription("Downloading file...");
        request.setTitle(fileName);
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);


        DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        if(downloadManager != null)
            downloadManager.enqueue(request);
    }

    @Override
    public void onForbiddenFileExtensionChange(List<String> fileExtensions) {
        this.forbiddenFileExtension = fileExtensions;
    }
}

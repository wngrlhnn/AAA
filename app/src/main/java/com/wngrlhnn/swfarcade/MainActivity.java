package com.wngrlhnn.swfarcade;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.view.*;
import android.webkit.*;
import androidx.webkit.WebViewAssetLoader;

public class MainActivity extends Activity {
    WebView web;
    ValueCallback<Uri[]> chooser;
    WebViewAssetLoader assetLoader;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(0xff0b0d12);
        getWindow().setNavigationBarColor(0xff0b0d12);

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);

        if (Build.VERSION.SDK_INT >= 16) {
            s.setAllowFileAccessFromFileURLs(true);
            s.setAllowUniversalAccessFromFileURLs(true);
        }

        assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        web.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                WebResourceResponse response = assetLoader.shouldInterceptRequest(request.getUrl());
                return response != null ? response : super.shouldInterceptRequest(view, request);
            }
            @SuppressWarnings("deprecation")
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                WebResourceResponse response = assetLoader.shouldInterceptRequest(Uri.parse(url));
                return response != null ? response : super.shouldInterceptRequest(view, url);
            }
        });

        web.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");

        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (chooser != null) chooser.onReceiveValue(null);
                chooser = cb;
                Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"*/*"});
                startActivityForResult(i, 42);
                return true;
            }
            @Override public void onShowCustomView(View v, CustomViewCallback cb) {
                setContentView(v);
            }
            @Override public void onHideCustomView() {
                setContentView(web);
            }
        });

        setContentView(web);
        web.loadUrl("https://appassets.androidplatform.net/assets/index.html");
    }

    class AndroidBridge {
        @JavascriptInterface public void setOrientation(String orientation) {
            runOnUiThread(() -> {
                if ("landscape".equals(orientation)) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                } else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
            });
        }

        @JavascriptInterface public void resetOrientation() {
            runOnUiThread(() -> setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED));
        }
    }

    private void sendVolumeKey(String key) {
        if (web == null) return;
        String js = "window.dispatchEvent(new KeyboardEvent('keydown',{key:'" + key + "',code:'" + key + "',bubbles:true,cancelable:true}));"
                  + "window.dispatchEvent(new KeyboardEvent('keyup',{key:'" + key + "',code:'" + key + "',bubbles:true,cancelable:true}));";
        web.evaluateJavascript(js, null);
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            sendVolumeKey("ArrowUp");
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            sendVolumeKey("ArrowDown");
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override protected void onActivityResult(int r, int c, Intent d) {
        super.onActivityResult(r, c, d);
        if (r == 42 && chooser != null) {
            chooser.onReceiveValue(c == RESULT_OK && d != null && d.getData() != null ? new Uri[]{d.getData()} : null);
            chooser = null;
        }
    }

    @Override public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
}

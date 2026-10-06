package com.wngrlhnn.swfarcade;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import android.graphics.Color;
import android.webkit.*;
import androidx.webkit.WebViewAssetLoader;

public class MainActivity extends Activity {
    WebView web;
    ValueCallback<Uri[]> chooser;
    WebViewAssetLoader assetLoader;
    FrameLayout fullscreenContainer;
    WebChromeClient.CustomViewCallback fullscreenCallback;
    long lastBackPress = 0;

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
                Intent i;
                try {
                    i = p.createIntent();
                } catch (Exception ex) {
                    i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                }
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/x-shockwave-flash", "application/octet-stream", "*/*"});
                i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                startActivityForResult(i, 42);
                return true;
            }
            @Override public void onShowCustomView(View v, CustomViewCallback cb) {
                fullscreenCallback = cb;
                fullscreenContainer = new FrameLayout(MainActivity.this);
                fullscreenContainer.setBackgroundColor(Color.BLACK);
                fullscreenContainer.addView(v, new FrameLayout.LayoutParams(-1, -1));
                setContentView(fullscreenContainer);
            }
            @Override public void onHideCustomView() {
                if (fullscreenContainer != null) {
                    fullscreenContainer.removeAllViews();
                    fullscreenContainer = null;
                }
                fullscreenCallback = null;
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
            if (c == RESULT_OK && d != null) {
                if (d.getClipData() != null) {
                    int n = d.getClipData().getItemCount();
                    Uri[] uris = new Uri[n];
                    for (int i = 0; i < n; i++) uris[i] = d.getClipData().getItemAt(i).getUri();
                    chooser.onReceiveValue(uris);
                } else if (d.getData() != null) {
                    Uri uri = d.getData();
                    persistReadPermission(uri);
                    chooser.onReceiveValue(new Uri[]{uri});
                } else chooser.onReceiveValue(null);
            } else chooser.onReceiveValue(null);
            chooser = null;
        }
    }

    private void persistReadPermission(Uri uri) {
        try {
            if (Build.VERSION.SDK_INT >= 19) {
                getContentResolver().takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                );
            }
        } catch (Exception ignored) {}
    }

    @Override public void onBackPressed() {
        if (fullscreenContainer != null) {
            if (fullscreenCallback != null) fullscreenCallback.onCustomViewHidden();
            else {
                fullscreenContainer = null;
                setContentView(web);
            }
            return;
        }
        if (web != null) {
            web.evaluateJavascript("(document.getElementById('playerView')?.style.display==='block')", value -> {
                if ("true".equals(value)) {
                    web.evaluateJavascript("(document.getElementById('gameMenu')?.classList.contains('open'))", menu -> {
                        if ("true".equals(menu)) {
                            web.evaluateJavascript("document.getElementById('gameMenu').classList.remove('open')", null);
                        } else {
                            web.evaluateJavascript("document.getElementById('back')?.click()", null);
                        }
                    });
                } else if (web.canGoBack()) {
                    web.goBack();
                } else {
                    confirmExit();
                }
            });
            return;
        }
        confirmExit();
    }

    private void confirmExit() {
        long now = System.currentTimeMillis();
        if (now - lastBackPress < 2000) {
            finish();
            return;
        }
        lastBackPress = now;
        Toast.makeText(this, "לחץ שוב על חזרה כדי לצאת", Toast.LENGTH_SHORT).show();
    }
}

package com.wngrlhnn.swfarcade;

import android.app.*;import android.os.*;import android.content.*;import android.net.Uri;import android.webkit.*;import android.view.*;import java.util.*;

public class MainActivity extends Activity {
    WebView web; ValueCallback<Uri[]> chooser;
    @Override public void onCreate(Bundle b){super.onCreate(b); getWindow().setStatusBarColor(0xff0b0d12); web=new WebView(this); WebSettings s=web.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setDatabaseEnabled(true); s.setAllowFileAccess(true); s.setAllowContentAccess(true); s.setMediaPlaybackRequiresUserGesture(false); web.setWebViewClient(new WebViewClient()); web.setWebChromeClient(new WebChromeClient(){
        @Override public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p){ if(chooser!=null) chooser.onReceiveValue(null); chooser=cb; Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("application/x-shockwave-flash"); i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"application/x-shockwave-flash","application/octet-stream"}); startActivityForResult(i,42); return true; }
        @Override public void onShowCustomView(View v, CustomViewCallback cb){ setContentView(v); }
        @Override public void onHideCustomView(){ setContentView(web); }
    }); setContentView(web); web.loadUrl("file:///android_asset/index.html"); }
    @Override protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d); if(r==42&&chooser!=null){chooser.onReceiveValue(c==RESULT_OK&&d!=null&&d.getData()!=null?new Uri[]{d.getData()}:null);chooser=null;}}
    @Override public void onBackPressed(){ if(web.canGoBack()) web.goBack(); else super.onBackPressed(); }
}

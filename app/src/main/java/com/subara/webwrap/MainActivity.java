package com.subara.webwrap;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

public class MainActivity extends Activity {

    public static final String PREFS = "webwrap_prefs";
    public static final String KEY_URL = "start_url";
    public static final String DEFAULT_URL = "https://www.pix.ai/en/";

    private WebView webView;
    private ProgressBar progressBar;
    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILE_CHOOSER_REQ = 1001;
    private String lastLoadedUrl = null;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout root = new FrameLayout(this);
        webView = new WebView(this);
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);

        root.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(progressBar, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(3)));
        setContentView(root);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);

        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        // 长按页面弹出「设置」入口
        webView.setOnLongClickListener(v -> {
            showMenu();
            return true;
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();

                // Google 登录相关域名一律交给 Custom Tabs（真实浏览器内核）
                if (isGoogleAuthUrl(url)) {
                    openCustomTab(url);
                    return true;
                }

                if (url.startsWith("http://") || url.startsWith("https://")) {
                    view.loadUrl(url);
                    return true;
                }
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                } catch (Exception ignored) {
                }
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
                progressBar.setProgress(newProgress);
            }

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> request.grant(request.getResources()));
            }

            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                filePathCallback = callback;
                try {
                    Intent intent = params.createIntent();
                    startActivityForResult(intent, FILE_CHOOSER_REQ);
                    return true;
                } catch (Exception e) {
                    filePathCallback = null;
                    return false;
                }
            }
        });

        loadStartUrl();
    }

    /** 判断是否为 Google OAuth 登录相关地址 */
    private boolean isGoogleAuthUrl(String url) {
        if (url == null) return false;
        String u = url.toLowerCase();
        return u.contains("accounts.google.com")
                || u.contains("accounts.youtube.com")
                || u.contains("/o/oauth2/")
                || u.contains("oauth2/auth")
                || u.contains("signin/v2")
                || u.contains("ServiceLogin")
                || u.contains("gsi/status");
    }

    /** 用系统浏览器 / Custom Tabs 打开登录页 */
    private void openCustomTab(String url) {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        i.addCategory(Intent.CATEGORY_BROWSABLE);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(i);
        } catch (Exception e) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (Exception ignored) {
            }
        }
    }

    private void showMenu() {
        String[] items = {"设置起始网址", "刷新页面", "翻译页面为中文", "还原原文"};
        new android.app.AlertDialog.Builder(this)
                .setTitle("操作")
                .setItems(items, (d, which) -> {
                    switch (which) {
                        case 0:
                            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                            break;
                        case 1:
                            webView.reload();
                            break;
                        case 2:
                            translatePage();
                            break;
                        case 3:
                            restorePage();
                            break;
                    }
                })
                .show();
    }

    /** 注入翻译脚本：把页面上的可达文本节点翻译成中文 */
    private void translatePage() {
        String js =
            "(function(){\n" +
            "  if(window.__pwTranslated) return 'already';\n" +
            "  window.__pwTranslated = true;\n" +
            "  window.__pwOrig = [];\n" +
            "  var SKIP={SCRIPT:1,STYLE:1,NOSCRIPT:1,CODE:1,PRE:1,TEXTAREA:1};\n" +
            "  function collect(root){\n" +
            "    var out=[];\n" +
            "    var w=document.createTreeWalker(root,NodeFilter.SHOW_TEXT,null);\n" +
            "    var n;\n" +
            "    while((n=w.nextNode())){\n" +
            "      var p=n.parentNode;\n" +
            "      if(!p||SKIP[p.nodeName]) continue;\n" +
            "      var t=n.nodeValue.trim();\n" +
            "      if(t.length<2||!/[A-Za-z]/.test(t)) continue;\n" +
            "      out.push(n);\n" +
            "    }\n" +
            "    return out;\n" +
            "  }\n" +
            "  var nodes=collect(document.body);\n" +
            "  var BATCH=8,i=0;\n" +
            "  function next(){\n" +
            "    if(i>=nodes.length){window.__pwDone&&window.__pwDone();return;}\n" +
            "    var slice=nodes.slice(i,i+BATCH);i+=BATCH;\n" +
            "    var texts=slice.map(function(n){return n.nodeValue;});\n" +
            "    var q='';\n" +
            "    for(var k=0;k<texts.length;k++){ if(k)q+='&q='; q+=encodeURIComponent(texts[k]); }\n" +
            "    fetch('https://translate.googleapis.com/translate_a/t?client=gtx&sl=auto&tl=zh-CN&'+q)\n" +
            "      .then(function(r){return r.json();})\n" +
            "      .then(function(d){\n" +
            "        var arr = Array.isArray(d)?d:(d&&d.sentences?d.sentences.map(function(x){return x.trans;}) : [d]);\n" +
            "        if(!Array.isArray(arr)) arr=[String(d)];\n" +
            "        for(var k=0;k<slice.length;k++){\n" +
            "          var tr=arr[k];\n" +
            "          if(Array.isArray(tr)) tr=tr[0];\n" +
            "          if(typeof tr==='string'&&tr.length){\n" +
            "            var node=slice[k];\n" +
            "            if(node.__pwOrig===undefined) node.__pwOrig=node.nodeValue;\n" +
            "            node.nodeValue=tr;\n" +
            "          }\n" +
            "        }\n" +
            "      })\n" +
            "      .catch(function(){})\n" +
            "      .then(function(){ setTimeout(next,120); });\n" +
            "  }\n" +
            "  next();\n" +
            "  return 'started';\n" +
            "})();";
        webView.evaluateJavascript(js, value -> {
            android.widget.Toast.makeText(this, "正在翻译…", android.widget.Toast.LENGTH_SHORT).show();
        });
    }

    /** 还原被翻译的文本 */
    private void restorePage() {
        String js =
            "(function(){\n" +
            "  var w=document.createTreeWalker(document.body,NodeFilter.SHOW_TEXT,null);\n" +
            "  var n,c=0;\n" +
            "  while((n=w.nextNode())){\n" +
            "    if(n.__pwOrig!==undefined){n.nodeValue=n.__pwOrig;delete n.__pwOrig;c++;}\n" +
            "  }\n" +
            "  window.__pwTranslated=false;\n" +
            "  return c;\n" +
            "})();";
        webView.evaluateJavascript(js, value ->
            android.widget.Toast.makeText(this, "已还原原文", android.widget.Toast.LENGTH_SHORT).show());
    }

    private void loadStartUrl() {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        String url = sp.getString(KEY_URL, DEFAULT_URL);
        if (url == null || url.trim().isEmpty()) url = DEFAULT_URL;
        url = normalize(url);
        lastLoadedUrl = url;
        webView.loadUrl(url);
    }

    static String normalize(String raw) {
        String u = raw.trim();
        if (!u.startsWith("http://") && !u.startsWith("https://")) {
            u = "https://" + u;
        }
        return u;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQ) {
            if (filePathCallback == null) return;
            Uri[] results = null;
            if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
                results = new Uri[]{data.getData()};
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView != null && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
        // 从设置页返回时，如果网址被改过就重新加载
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        String want = normalize(sp.getString(KEY_URL, DEFAULT_URL));
        if (lastLoadedUrl == null) {
            lastLoadedUrl = want;
            webView.loadUrl(want);
        } else if (!lastLoadedUrl.equals(want)) {
            lastLoadedUrl = want;
            webView.loadUrl(want);
        }
    }

    @Override
    protected void onPause() {
        if (webView != null) webView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}

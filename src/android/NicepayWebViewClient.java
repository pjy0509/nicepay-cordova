package kr.nicepay.cordova;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

abstract public class NicepayWebViewClient extends WebViewClient {
    Activity activity;
    WebView webView;

    protected ArrayList<String> endpoints;
    protected Boolean isWebViewLoaded = false;

    protected final static String MARKET_PREFIX = "market://details?id=";
    protected final static String PLAY_GOOGLE_BY_ID = "https://play.google.com/store/apps/details?id=";
    protected final static String PLAY_GOOGLE_BY_SEARCH_KEYWORD = "https://play.google.com/store/search?q=";

    public NicepayWebViewClient(Activity activity, ArrayList<String> endpoints) {
        this.activity = activity;
        this.endpoints = endpoints;
    }

    @Override
    public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
        onChangeWebviewURL(view, url);
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        return onChangeWebviewURL(view, url);
    }

    abstract public void onPageFinished(WebView view, String url);

    protected boolean onChangeWebviewURL(WebView view, String url) {
        this.webView = view;

        if (isEnd(url)) {
            return callback(
                    new HashMap<String, Object>() {{
                        put("status", "success");
                        put("responseCode", "100");
                        put("message", "결제 완료");
                    }}
            );
        } else if (isFail(url)) {
            return callback(
                    new HashMap<String, Object>() {{
                        put("status", "fail");

                        try {
                            Map<String, Object> parsed = ConvertUtils.convertURLQueryParameter2Map(new URL(url));
                            put("responseCode", parsed.get("errCd"));
                            put("message", parsed.get("errMsg"));
                        } catch (MalformedURLException e) {
                            put("responseCode", "");
                            put("message", "");
                        }
                    }}
            );
        } else if (isApp(url)) {
            return openApp(url);
        } else {
            return false;
        }
    }

    protected boolean isEnd(String url) {
        boolean isContinue = true;
        Iterator<String> endpointIterator = endpoints.iterator();
        while (endpointIterator.hasNext() && isContinue) {
            String endpoint = endpointIterator.next();
            isContinue = !url.startsWith(endpoint);
        }
        return !isContinue;
    }

    protected boolean isFail(String url) {
        return url.startsWith("https://web.nicepay.co.kr/v3/smart/common/error.jsp");
    }

    protected boolean isApp(String url) {
        return !url.startsWith("http") &&
                !url.startsWith("https") &&
                !url.startsWith("about:blank") &&
                !url.startsWith("file") &&
                !url.startsWith("javascript:");
    }

    protected boolean openApp(String url) {
        Intent intent = null;

        try {
            intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);

            if (intent == null) return false;

            startNewActivity(intent.getDataString());
        } catch (URISyntaxException e) {
            return false;
        } catch (ActivityNotFoundException e) {
            if (intent == null) return false;

            String packageName = intent.getPackage();

            if (packageName == null) return false;

            startNewActivity(MARKET_PREFIX + packageName);
        }
        return true;
    }

    protected void startNewActivity(String parsingUri) {
        Uri uri = Uri.parse(parsingUri);

        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException e) {
            if (parsingUri.startsWith(MARKET_PREFIX))
                activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_GOOGLE_BY_ID + parsingUri.replace(MARKET_PREFIX, ""))));
            else
                activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_GOOGLE_BY_SEARCH_KEYWORD + uri.getScheme())));
        }
    }

    protected boolean callback(Map<String, Object> response) {
        Intent data = new Intent();

        data.putExtra("response", ConvertUtils.convertMap2JSONString(response));
        activity.setResult(NicepayCordova.REQUEST_CODE, data);
        activity.finish();

        return true;
    }
}
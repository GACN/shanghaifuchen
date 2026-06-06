package com.xiaoguo.shanghaifuchen;

import android.app.*;
import android.os.*;
import android.webkit.*;
import android.content.*;
import android.view.*;
import android.net.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MainActivity extends Activity {
    private WebView web;
    private SharedPreferences prefs;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("shanghaifc_store", MODE_PRIVATE);
        web = new WebView(this);
        web.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        setContentView(web);
        WebSettings st = web.getSettings();
        st.setJavaScriptEnabled(true);
        st.setDomStorageEnabled(true);
        st.setDatabaseEnabled(true);
        st.setAllowFileAccess(true);
        st.setAllowContentAccess(true);
        st.setLoadWithOverviewMode(true);
        st.setUseWideViewPort(true);
        st.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        st.setCacheMode(WebSettings.LOAD_DEFAULT);
        st.setLoadsImagesAutomatically(true);
        st.setMediaPlaybackRequiresUserGesture(false);
        if (Build.VERSION.SDK_INT >= 19) WebView.setWebContentsDebuggingEnabled(false);
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient());
        web.addJavascriptInterface(new GameBridge(), "GameNative");
        web.loadUrl("file:///android_asset/game/index.html");
    }

    @Override public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack(); else super.onBackPressed();
    }

    public class GameBridge {
        @JavascriptInterface public String preload() {
            try {
                JSONObject obj = new JSONObject();
                Map<String, ?> all = prefs.getAll();
                for (String k: all.keySet()) if (k.startsWith("shfc_") || k.startsWith("shanghai")) obj.put(k, String.valueOf(all.get(k)));
                return obj.toString();
            } catch(Exception e) { return "{}"; }
        }
        @JavascriptInterface public String getItem(String key) { return prefs.getString(key, null); }
        @JavascriptInterface public void setItem(String key, String value) { prefs.edit().putString(key, value).apply(); }
        @JavascriptInterface public void removeItem(String key) { prefs.edit().remove(key).apply(); }

        @JavascriptInterface public String listModels(String base, String key, String fallbackModel) {
            if (base == null || base.trim().isEmpty()) return jsonErr("请先填写中转站 Base URL");
            try {
                String txt = httpJson("GET", openAiBase(base)+"/v1/models", key, null, 20000);
                JSONObject in = new JSONObject(txt);
                JSONArray out = new JSONArray();
                if (in.has("data")) {
                    JSONArray data = in.getJSONArray("data");
                    for (int i=0;i<data.length();i++) {
                        Object item = data.get(i); String id = null;
                        if (item instanceof JSONObject) id = ((JSONObject)item).optString("id", null); else id = String.valueOf(item);
                        if (id != null && id.length() > 0) { JSONObject m = new JSONObject(); m.put("name", id); m.put("model", id); out.put(m); }
                    }
                }
                if (out.length()==0 && fallbackModel != null && fallbackModel.length()>0) { JSONObject m=new JSONObject(); m.put("name", fallbackModel); m.put("model", fallbackModel); out.put(m); }
                JSONObject res = new JSONObject(); res.put("models", out); return res.toString();
            } catch(Exception e) { return jsonErr(e.getMessage()); }
        }

        @JavascriptInterface public String generate(String base, String key, String model, String prompt, double temperature, int maxTokens) {
            if (base == null || base.trim().isEmpty()) return jsonErr("请先在设置里填写中转站 Base URL");
            try {
                JSONObject body = new JSONObject();
                body.put("model", model == null || model.isEmpty() ? "gpt-5.5" : model);
                JSONArray msgs = new JSONArray();
                JSONObject sys = new JSONObject(); sys.put("role", "system"); sys.put("content", "你是中文商业模拟游戏“商海浮沉”的叙事引擎。输出简洁、有画面感，不要解释模型身份。"); msgs.put(sys);
                JSONObject user = new JSONObject(); user.put("role", "user"); user.put("content", prompt == null ? "" : prompt); msgs.put(user);
                body.put("messages", msgs); body.put("temperature", temperature); body.put("max_tokens", maxTokens); body.put("stream", false);
                String txt = httpJson("POST", openAiBase(base)+"/v1/chat/completions", key, body.toString(), Math.max(30000, 120000));
                JSONObject in = new JSONObject(txt); String content = "";
                JSONArray choices = in.optJSONArray("choices");
                if (choices != null && choices.length()>0) {
                    JSONObject c = choices.getJSONObject(0);
                    JSONObject msg = c.optJSONObject("message");
                    if (msg != null) content = msg.optString("content", ""); else content = c.optString("text", "");
                }
                JSONObject res = new JSONObject(); res.put("response", content); res.put("done", true); return res.toString();
            } catch(Exception e) { return jsonErr(e.getMessage()); }
        }

        private String jsonErr(String msg) { try { JSONObject o=new JSONObject(); o.put("error", msg == null ? "error" : msg); o.put("response", ""); return o.toString(); } catch(Exception e) { return "{\"error\":\"error\",\"response\":\"\"}"; } }
        private String trimSlash(String s) { s=s.trim(); while(s.endsWith("/")) s=s.substring(0,s.length()-1); return s; }
        private String openAiBase(String s) {
            s = trimSlash(s == null ? "" : s);
            if (s.endsWith("/v1")) s = s.substring(0, s.length() - 3);
            return s;
        }
        private String httpJson(String method, String url, String key, String body, int timeoutMs) throws Exception {
            HttpURLConnection c = (HttpURLConnection)new URL(url).openConnection();
            c.setConnectTimeout(timeoutMs); c.setReadTimeout(timeoutMs); c.setRequestMethod(method);
            c.setRequestProperty("Content-Type", "application/json"); c.setRequestProperty("Accept", "application/json"); c.setRequestProperty("User-Agent", "ShangHaiFuChen-APK/1.0");
            if (key != null && key.trim().length() > 0) c.setRequestProperty("Authorization", "Bearer "+key.trim());
            if (body != null) { c.setDoOutput(true); try(OutputStream os=c.getOutputStream()) { os.write(body.getBytes(StandardCharsets.UTF_8)); } }
            int code = c.getResponseCode(); InputStream is = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream(); byte[] buf = new byte[8192]; int n;
            while((n=is.read(buf))!=-1) baos.write(buf,0,n);
            String txt = baos.toString("UTF-8"); if (code < 200 || code >= 300) throw new IOException("HTTP "+code+": "+txt); return txt;
        }
    }
}

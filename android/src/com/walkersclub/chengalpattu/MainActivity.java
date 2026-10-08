package com.walkersclub.chengalpattu;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** Walkers Welfare Club: the whole app ships inside the APK and runs in a WebView; registrations are stored on the phone. */
public class MainActivity extends Activity {
    private static final int REQ_GALLERY = 1, REQ_CAMERA = 2;
    private WebView web;
    private ValueCallback<Uri[]> pendingPick;
    private File cameraFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#1C3FD8"));
        setContentView(web);
        paintBars("#1C3FD8", false);

        WebSettings ws = web.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setAllowFileAccess(true);
        ws.setAllowFileAccessFromFileURLs(true);
        ws.setMediaPlaybackRequiresUserGesture(false);
        ws.setTextZoom(100);
        ws.setSupportZoom(false);
        ws.setBuiltInZoomControls(false);
        ws.setDisplayZoomControls(false);

        web.addJavascriptInterface(new Bridge(), "WWCNative");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                if ("file".equals(u.getScheme())) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) { }
                return true;
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (pendingPick != null) pendingPick.onReceiveValue(null);
                pendingPick = callback;
                try {
                    if (params.isCaptureEnabled()) openCamera(); else openGallery();
                    return true;
                } catch (Exception e) {
                    pendingPick = null;
                    return false;
                }
            }
        });
        WebView.setWebContentsDebuggingEnabled(true);

        if (savedInstanceState != null) web.restoreState(savedInstanceState);
        else web.loadUrl("file:///android_asset/www/index.html");
    }

    private void openGallery() {
        Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
        pick.addCategory(Intent.CATEGORY_OPENABLE);
        pick.setType("image/*");
        pick.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/jpeg", "image/png", "image/webp"});
        startActivityForResult(Intent.createChooser(pick, "Choose your photo"), REQ_GALLERY);
    }

    private void openCamera() {
        cameraFile = FilesProvider.file(this, "selfie-" + System.currentTimeMillis() + ".jpg");
        Uri out = FilesProvider.uri(cameraFile.getName());
        Intent cam = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cam.putExtra(MediaStore.EXTRA_OUTPUT, out);
        cam.setClipData(ClipData.newRawUri("photo", out));
        cam.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(cam, REQ_CAMERA);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (pendingPick == null) return;
        Uri[] result = null;
        if (resultCode == RESULT_OK) {
            if (requestCode == REQ_GALLERY && data != null && data.getData() != null) result = new Uri[]{data.getData()};
            else if (requestCode == REQ_CAMERA && cameraFile != null && cameraFile.length() > 0) result = new Uri[]{Uri.fromFile(cameraFile)};
        }
        pendingPick.onReceiveValue(result);
        pendingPick = null;
    }

    @Override
    public void onBackPressed() {
        web.evaluateJavascript("window.wwcBack ? wwcBack() : false", value -> {
            if (!"true".equals(value)) MainActivity.super.onBackPressed();
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        web.saveState(outState);
    }

    private void paintBars(String hex, boolean lightBackground) {
        Window w = getWindow();
        int c;
        try { c = Color.parseColor(hex); } catch (IllegalArgumentException e) { return; }
        w.setStatusBarColor(c);
        w.setNavigationBarColor(c);
        View d = w.getDecorView();
        int flags = d.getSystemUiVisibility();
        int light = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | (Build.VERSION.SDK_INT >= 26 ? View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR : 0);
        d.setSystemUiVisibility(lightBackground ? (flags | light) : (flags & ~light));
    }

    private static String read(File f) throws IOException {
        try (InputStream in = new FileInputStream(f)) {
            byte[] buf = new byte[(int) f.length()];
            int off = 0, n;
            while (off < buf.length && (n = in.read(buf, off, buf.length - off)) > 0) off += n;
            return new String(buf, 0, off, StandardCharsets.UTF_8);
        }
    }

    private static void write(File f, String text) throws IOException {
        File tmp = new File(f.getPath() + ".tmp");
        try (OutputStream out = new FileOutputStream(tmp)) { out.write(text.getBytes(StandardCharsets.UTF_8)); }
        if (!tmp.renameTo(f)) { f.delete(); if (!tmp.renameTo(f)) throw new IOException("rename failed"); }
    }

    /** Methods the page calls as window.WWCNative.*. */
    private class Bridge {
        private File store() { return new File(getFilesDir(), "members.json"); }

        @JavascriptInterface
        public String load() {
            try { return store().exists() ? read(store()) : "[]"; } catch (IOException e) { return "[]"; }
        }

        @JavascriptInterface
        public boolean save(String json) {
            try { write(store(), json); return true; } catch (IOException e) { return false; }
        }

        @JavascriptInterface
        public void copy(String text) {
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("Walkers Welfare Club", text));
        }

        @JavascriptInterface
        public void share(String fileName, String text) throws IOException {
            File f = FilesProvider.file(MainActivity.this, fileName);
            write(f, text);
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType(fileName.endsWith(".csv") ? "text/csv" : "text/plain");
            send.putExtra(Intent.EXTRA_STREAM, FilesProvider.uri(f.getName()));
            send.putExtra(Intent.EXTRA_SUBJECT, "Walkers Welfare Club members 2026-27");
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            runOnUiThread(() -> startActivity(Intent.createChooser(send, "Share member list")));
        }

        /** Shares a binary file (e.g. the Excel export) sent from the page as base64. */
        @JavascriptInterface
        public void shareFile(String fileName, String base64) throws IOException {
            File f = FilesProvider.file(MainActivity.this, fileName);
            try (OutputStream out = new FileOutputStream(f)) { out.write(android.util.Base64.decode(base64, android.util.Base64.DEFAULT)); }
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType(FilesProvider.mime(fileName));
            send.putExtra(Intent.EXTRA_STREAM, FilesProvider.uri(f.getName()));
            send.putExtra(Intent.EXTRA_SUBJECT, "Walkers Welfare Club members 2026-27");
            send.setClipData(ClipData.newRawUri(fileName, FilesProvider.uri(f.getName())));
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            runOnUiThread(() -> startActivity(Intent.createChooser(send, "Share " + fileName)));
        }

        @JavascriptInterface
        public void bars(String hex, boolean lightBackground) {
            runOnUiThread(() -> paintBars(hex, lightBackground));
        }
    }
}

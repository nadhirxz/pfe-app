package com.delivery.driver;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieSyncManager;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainActivity extends AppCompatActivity {
    private MyWebView webview;
    private SwipeRefreshLayout swipe;
    private String LINK = "https://pfeusthb.herokuapp.com/drivers";
    private String VERSION = "1.0 (Driver)";
    private String RELEASE_DATE = "01/07/2021";
    private static int notification_id = 0;

    public class GeoWebChromeClient extends WebChromeClient {
        @Override
        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            // Always grant permission since the app itself requires location
            // permission and the user has therefore already granted it
            callback.invoke(origin, true, false);
        }
    }
    private class CustomWebViewClient extends WebViewClient {
        @Override
        public  boolean shouldOverrideUrlLoading(WebView view, String url) {
            Uri uri = Uri.parse(url);
            if (uri.getHost() != null && uri.getHost().equals(Uri.parse(LINK).getHost())) {
                return false;
            }
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            findViewById(R.id.imageLoading).setVisibility(View.VISIBLE);
            findViewById(R.id.webview).setVisibility(View.GONE);
            swipe.setEnabled(true);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            CookieSyncManager.getInstance().sync();
            swipe.setRefreshing(false);
            findViewById(R.id.imageLoading).setVisibility(View.GONE);
            findViewById(R.id.webview).setVisibility(View.VISIBLE);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            findViewById(R.id.imageLoading).setVisibility(View.GONE);
            webview.loadUrl("file:///android_asset/error.html");
        }

        @TargetApi(android.os.Build.VERSION_CODES.M)
        @Override
        public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError rerr) {
            // Redirect to deprecated method, so you can use it in all SDK versions
            onReceivedError(view, rerr.getErrorCode(), rerr.getDescription().toString(), req.getUrl().toString());
        }
    }

    public static final String CHANNEL_ID = "channel";
    private NotificationManagerCompat notificationManager;

    @Override
    protected void onPause() {
        super.onResume();
    }

    @SuppressLint({"AddJavascriptInterface", "SetJavaScriptEnabled"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        notificationManager = NotificationManagerCompat.from(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        }, 0);

        swipe = (SwipeRefreshLayout) findViewById(R.id.swipe);
        swipe.setOnRefreshListener(
            new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    webview.reload();
                }
            }
        );
        swipe.setColorSchemeColors(getResources().getColor(R.color.colorPrimary));

        webview = (MyWebView) findViewById(R.id.webview);

        webview.setWebViewClient(new CustomWebViewClient());
        webview.setWebChromeClient(new GeoWebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback)  {
                callback.invoke(origin, true, false);
            }
        });
        WebSettings webSettings = webview.getSettings();

        webSettings.setJavaScriptEnabled(true);
        webSettings.setGeolocationEnabled(true);

        webSettings.setJavaScriptEnabled(true);
        webSettings.setAppCachePath(getApplicationContext().getFilesDir().getAbsolutePath() + "/cache");

        CookieSyncManager.getInstance().startSync();
        CookieSyncManager.getInstance().sync();

        webview.addJavascriptInterface(new WebAppInterface(this), "Android");
        webview.loadUrl(LINK);
    }
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Channel";
            String description = "Notification Channel";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    private void addNotification(String title, String message) {
        createNotificationChannel();
        // Builds your notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.notification)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.notification))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message));

        // Creates the intent needed to show the notification
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIntent);
        builder.setAutoCancel(true);

        // Add as notification
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(notification_id++, builder.build());
    }
    public class WebAppInterface {
        Context mContext;
        WebAppInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public void showNotification(String title, String message) {
            addNotification(title, message);
        }
        @JavascriptInterface
        public String getAppVersion() {
            return VERSION;
        }
        @JavascriptInterface
        public void disableRefresh() {
            swipe.setEnabled(false);
        }
    }
    public void onBackPressed() {
        if (webview.canGoBack()) {
            webview.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
package com.app.Ritmo96.fragments;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.app.Ritmo96.R;
import com.app.Ritmo96.activities.MainActivity;
import com.app.Ritmo96.utils.Constant;

public class FragmentWebView extends DialogFragment {

    public static final String TAG = "FragmentWebView";
    private Toolbar toolbar;
    private RelativeLayout parentView;
    private ImageButton btnBack;
    private TextView toolbarTitle;
    View rootView;
    WebView webView;
    ProgressBar progressBar;
    Button btnFailedRetry;
    View lytFailed;
    String strUrl;
    String strTitle;
    MainActivity activity;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        activity = (MainActivity) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_webview, container, false);
        initView();
        loadData();
        setupToolbar();
        return rootView;
    }

    private void initView() {
        if (getArguments() != null) {
            strTitle = getArguments().getString("title");
            strUrl = getArguments().getString("url");
        }

        parentView = rootView.findViewById(R.id.fragment_view);
        ViewCompat.setOnApplyWindowInsetsListener(parentView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        toolbar = rootView.findViewById(R.id.toolbar);
        toolbarTitle = rootView.findViewById(R.id.toolbar_title);
        btnBack = rootView.findViewById(R.id.btn_back);

        webView = rootView.findViewById(R.id.webView);
        progressBar = rootView.findViewById(R.id.progressBar);
        btnFailedRetry = rootView.findViewById(R.id.btn_failed_retry);
        lytFailed = rootView.findViewById(R.id.lyt_failed);
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void loadData() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setBuiltInZoomControls(false);
        webView.getSettings().setSupportZoom(true);
        webView.setWebViewClient(new CustomWebViewClient());
        webView.loadUrl(strUrl);

        webView.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                WebView webView = (WebView) v;
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    if (webView.canGoBack()) {
                        webView.goBack();
                        return true;
                    }
                }
            }
            return false;
        });

        FrameLayout customViewContainer = rootView.findViewById(R.id.customViewContainer);
        webView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int newProgress) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    progressBar.setProgress(newProgress, true);
                } else {
                    progressBar.setProgress(newProgress);
                }
                if (newProgress == 100) {
                    progressBar.setVisibility(View.INVISIBLE);
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                }
            }

            public void onShowCustomView(View view, CustomViewCallback callback) {
                super.onShowCustomView(view, callback);
                webView.setVisibility(View.INVISIBLE);
                customViewContainer.setVisibility(View.VISIBLE);
                customViewContainer.addView(view);
            }

            public void onHideCustomView() {
                super.onHideCustomView();
                webView.setVisibility(View.VISIBLE);
                customViewContainer.setVisibility(View.GONE);
            }
        });

    }

    private class CustomWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);

            actionHandler("mailto:", Intent.ACTION_SENDTO, url);
            actionHandler("sms:", Intent.ACTION_SENDTO, url);
            actionHandler("tel:", Intent.ACTION_DIAL, url);

            socialHandler(url, "intent://instagram", "com.instagram.android");
            socialHandler(url, "instagram://", "com.instagram.android");
            socialHandler(url, "instagram.com", "com.instagram.android");
            socialHandler(url, "twitter://", "com.twitter.android");
            socialHandler(url, "twitter.com", "com.twitter.android");
            socialHandler(url, "facebook.com", "com.facebook.katana");
            socialHandler(url, "maps.google.com", "com.google.android.apps.maps");
            socialHandler(url, "api.whatsapp.com", "com.whatsapp");
            socialHandler(url, "https://play.google.com/store/apps/details?id=", null);

            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            progressBar.setVisibility(View.GONE);
        }

        @SuppressWarnings("deprecation")
        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(activity, getResources().getString(R.string.failed_text), Toast.LENGTH_LONG).show();
            lytFailed.setVisibility(View.VISIBLE);
        }
    }

    public void actionHandler(String type, String action, String url) {
        if (url != null && url.startsWith(type)) {
            Intent intent = new Intent(action, Uri.parse(url));
            startActivity(intent);
        }
    }

    public void socialHandler(String url, String social_url, String package_name) {
        PackageManager packageManager = activity.getPackageManager();
        if (url != null && url.contains(social_url)) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            try {
                intent.setPackage(package_name);
                intent.setData(Uri.parse(url));
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, "error : " + e.getMessage());
            }
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    private void setupToolbar() {
        toolbarTitle.setText(strTitle);
        btnBack.setOnClickListener(v -> new Handler(Looper.getMainLooper()).postDelayed(() -> {
            FragmentManager fm = activity.getSupportFragmentManager();
            if (fm.getBackStackEntryCount() > 0) {
                fm.popBackStack();
            }
            dismiss();
        }, Constant.DELAY_ACTION_CLICK));
        themeColor();
    }

    private void themeColor() {
        parentView.setBackgroundColor(ContextCompat.getColor(activity, R.color.color_light_status_bar));
        toolbar.setBackgroundColor(ContextCompat.getColor(activity, R.color.color_light_primary));
        toolbarTitle.setTextColor(ContextCompat.getColor(activity, R.color.color_white));
        btnBack.setColorFilter(ContextCompat.getColor(activity, R.color.color_white), PorterDuff.Mode.SRC_ATOP);
    }

}
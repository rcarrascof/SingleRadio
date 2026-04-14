package com.app.AlofokeFm.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.appcompat.app.AppCompatActivity;

import com.app.AlofokeFm.R;
import com.app.AlofokeFm.utils.Constant;
import com.google.android.material.snackbar.Snackbar;
import com.solodroidx.ads.appopen.AppOpenAd;

public class ActivityRedirect extends AppCompatActivity {

    ImageButton btnClose;
    Button btnRedirect;
    String redirectUrl = "";
    OnBackPressedDispatcher onBackPressedDispatcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_redirect);
        redirectUrl = getIntent().getStringExtra("redirect_url");
        initView();
        handleOnBackPressed();
    }

    public void handleOnBackPressed() {
        onBackPressedDispatcher = getOnBackPressedDispatcher();
        onBackPressedDispatcher.addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
                AppOpenAd.isAppOpenAdLoaded = false;
            }
        });
    }

    private void initView() {
        btnClose = findViewById(R.id.btn_close);
        btnRedirect = findViewById(R.id.btn_redirect);

        btnClose.setOnClickListener(view -> {
            finish();
            AppOpenAd.isAppOpenAdLoaded = false;
        });

        btnRedirect.setOnClickListener(view -> {
            if (redirectUrl.equals("")) {
                Snackbar.make(findViewById(android.R.id.content), getString(R.string.redirect_error), Snackbar.LENGTH_SHORT).show();
            } else {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(redirectUrl)));
                finish();
                AppOpenAd.isAppOpenAdLoaded = false;
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AppOpenAd.isAppOpenAdLoaded = false;
        Constant.isRadioPlaying = false;
    }

}

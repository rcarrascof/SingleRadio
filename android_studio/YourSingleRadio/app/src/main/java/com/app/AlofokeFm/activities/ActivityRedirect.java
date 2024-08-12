package com.app.AlofokeFm.activities;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.app.AlofokeFm.R;
import com.app.AlofokeFm.utils.Constant;
import com.google.android.material.snackbar.Snackbar;

/**
 * Created by Reinold Carrasco
 * on 9/5/2022
 */
public class ActivityRedirect extends AppCompatActivity {

    ImageButton btnClose;
    Button btnRedirect;
    String redirectUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_redirect);
        redirectUrl = getIntent().getStringExtra("redirect_url");
        initView();
    }

    private void initView() {
        btnClose = findViewById(R.id.btn_close);
        btnRedirect = findViewById(R.id.btn_redirect);

        btnClose.setOnClickListener(view -> {
            finish();
            Constant.isAppOpen = false;
        });

        btnRedirect.setOnClickListener(view -> {
            if (redirectUrl.equals("")) {
                Snackbar.make(findViewById(android.R.id.content), getString(R.string.redirect_error), Snackbar.LENGTH_SHORT).show();
            } else {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(redirectUrl)));
                finish();
                Constant.isAppOpen = false;
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Constant.isAppOpen = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Constant.isAppOpen = false;
        Constant.isRadioPlaying = false;
    }

}





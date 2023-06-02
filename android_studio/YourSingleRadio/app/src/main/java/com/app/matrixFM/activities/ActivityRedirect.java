package com.app.matrixFM.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.app.matrixFM.R;
import com.app.matrixFM.database.prefs.SharedPref;
import com.google.android.material.snackbar.Snackbar;

/**
 * Created by Reinold Carrasco
 * on 9/5/2022
 */
public class ActivityRedirect extends AppCompatActivity {

    SharedPref sharedPref;
    ImageButton btnClose;
    Button btnRedirect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_redirect);
        sharedPref = new SharedPref(this);
        initView();
    }

    private void initView() {
        btnClose = findViewById(R.id.btn_close);
        btnRedirect = findViewById(R.id.btn_redirect);

        btnClose.setOnClickListener(view -> finish());

        btnRedirect.setOnClickListener(view -> {
            if (sharedPref.getRedirectUrl().equals("")) {
                Snackbar.make(findViewById(android.R.id.content), getString(R.string.redirect_error), Snackbar.LENGTH_SHORT).show();
            } else {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(sharedPref.getRedirectUrl())));
                finish();
            }
        });
    }

}



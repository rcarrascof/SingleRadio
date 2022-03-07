package com.app.AlofokeFm.activities;

import static com.app.AlofokeFm.utils.Constant.PERMISSIONS_REQUEST;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Html;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.app.AlofokeFm.R;
import com.app.AlofokeFm.database.prefs.SharedPref;
import com.app.AlofokeFm.utils.Tools;

public class ActivityPermission extends AppCompatActivity {

    Button btn_allow_permission;
    Button btn_later;
    TextView txt_permission_message;
    SharedPref sharedPref;
    ScrollView scroll_view;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.darkStatusBar(this, false);
        setContentView(R.layout.activity_permission);

        sharedPref = new SharedPref(this);

        btn_allow_permission = findViewById(R.id.btn_allow_permission);
        txt_permission_message = findViewById(R.id.txt_permission_message);
        scroll_view = findViewById(R.id.scroll_view);

        btn_allow_permission.setOnClickListener(v -> Tools.requestPermission(ActivityPermission.this));
        txt_permission_message.setText(Html.fromHtml(getString(R.string.permission_message)));

        btn_later = findViewById(R.id.btn_later);
        btn_later.setOnClickListener(v -> finish());

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                finishAffinity();
            }
        }
    }

}

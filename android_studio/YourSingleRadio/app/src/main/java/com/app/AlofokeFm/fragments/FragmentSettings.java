package com.app.AlofokeFm.fragments;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.app.AlofokeFm.BuildConfig;
import com.app.AlofokeFm.R;
import com.app.AlofokeFm.activities.ActivityPermission;
import com.app.AlofokeFm.activities.MainActivity;
import com.app.AlofokeFm.database.prefs.SharedPref;
import com.app.AlofokeFm.utils.Constant;
import com.app.AlofokeFm.utils.Tools;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.solodroid.push.sdk.provider.OneSignalPush;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.text.DecimalFormat;

public class FragmentSettings extends DialogFragment {

    private Toolbar toolbar;
    private LinearLayout parentView;
    private ImageButton btnBack;
    private TextView toolbarTitle;
    TextView txt_cache_size;
    private View rootView;
    private MainActivity activity;
    SharedPref sharedPref;
    LinearLayout btnPermission;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        activity = (MainActivity) context;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_settings, container, false);
        sharedPref = new SharedPref(activity);
        initView();
        setupToolbar();
        return rootView;
    }

    @SuppressLint("RtlHardcoded")
    private void initView() {
        parentView = rootView.findViewById(R.id.fragment_view);
        ViewCompat.setOnApplyWindowInsetsListener(parentView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        toolbar = rootView.findViewById(R.id.toolbar);
        toolbarTitle = rootView.findViewById(R.id.toolbar_title);
        btnBack = rootView.findViewById(R.id.btn_back);
        btnPermission = rootView.findViewById(R.id.btn_permission);

        RelativeLayout btnNotification = rootView.findViewById(R.id.btn_notification);
        btnNotification.setOnClickListener(v -> {
            Intent intent = new Intent();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, BuildConfig.APPLICATION_ID);
            } else {
                intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
                intent.putExtra("app_package", BuildConfig.APPLICATION_ID);
                intent.putExtra("app_uid", activity.getApplicationInfo().uid);
            }
            startActivity(intent);
        });

        if (new OneSignalPush.Builder(activity).getSdkName().equals("no-notification-sdk")) {
            btnNotification.setVisibility(View.GONE);
        } else {
            btnNotification.setVisibility(View.VISIBLE);
        }

        txt_cache_size = rootView.findViewById(R.id.txt_cache_size);
        initializeCache();

        rootView.findViewById(R.id.lyt_clear_cache).setOnClickListener(v -> clearCache());

        rootView.findViewById(R.id.btn_privacy_policy).setOnClickListener(v -> {
            FragmentWebView fragment = new FragmentWebView();
            Bundle args = new Bundle();
            args.putString("title", getString(R.string.title_setting_privacy));
            args.putString("url", sharedPref.getPrivacyPolicyUrl());
            fragment.setArguments(args);
            FragmentManager fragmentManager = activity.getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction().setCustomAnimations(R.anim.slide_up, 0, 0, R.anim.slide_down);
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            transaction.add(R.id.fragment_container, fragment).addToBackStack("privacy_policy");
            transaction.commit();
        });

        rootView.findViewById(R.id.btn_rate).setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID))));

        rootView.findViewById(R.id.btn_share).setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
            intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_content) + "\n" + "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID);
            intent.setType("text/plain");
            startActivity(intent);
        });

        rootView.findViewById(R.id.btn_more).setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(sharedPref.getMoreAppsUrl()))));

        rootView.findViewById(R.id.btn_about).setOnClickListener(v -> {
            LayoutInflater layoutInflater = LayoutInflater.from(activity);
            View view = layoutInflater.inflate(R.layout.custom_dialog_about, null);
            TextView txtAppVersion = view.findViewById(R.id.txt_app_version);
            txtAppVersion.setText(getString(R.string.msg_about_version) + " " + BuildConfig.VERSION_NAME);
            final MaterialAlertDialogBuilder alert = new MaterialAlertDialogBuilder(activity);
            alert.setView(view);
            AlertDialog alertDialog = alert.create();
            Tools.dialogButtonSelected(activity, view, alertDialog, () -> {
            });
            alertDialog.show();
        });

        btnPermission.setOnClickListener(view -> startActivity(new Intent(activity, ActivityPermission.class)));
        permissionVisibility();

    }

    public void permissionVisibility() {
        if ((ContextCompat.checkSelfPermission(activity, "android.permission.READ_PHONE_STATE") == PackageManager.PERMISSION_GRANTED)) {
            btnPermission.setVisibility(View.GONE);
        } else {
            btnPermission.setVisibility(View.VISIBLE);
        }
    }

    private void clearCache() {
        LayoutInflater layoutInflater = LayoutInflater.from(activity);
        View view = layoutInflater.inflate(R.layout.dialog_custom, null);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
        builder.setView(view);
        builder.setTitle(getString(R.string.title_setting_clear_cache));
        builder.setMessage(getString(R.string.msg_clear_cache));
        AlertDialog alertDialog = builder.create();
        Tools.dialogButtonSelected(activity, view, alertDialog, () -> {
            FileUtils.deleteQuietly(activity.getCacheDir());
            FileUtils.deleteQuietly(activity.getExternalCacheDir());
            txt_cache_size.setText(getString(R.string.sub_setting_clear_cache_start) + " 0 Bytes " + getString(R.string.sub_setting_clear_cache_end));
            Snackbar.make(activity.findViewById(android.R.id.content), getString(R.string.msg_cache_cleared), Snackbar.LENGTH_SHORT).show();
        });
        alertDialog.show();
    }

    private void initializeCache() {
        txt_cache_size.setText(getString(R.string.sub_setting_clear_cache_start) + " " + readableFileSize((0 + getDirSize(activity.getCacheDir())) + getDirSize(activity.getExternalCacheDir())) + " " + getString(R.string.sub_setting_clear_cache_end));
    }

    @SuppressWarnings("ConstantConditions")
    public long getDirSize(File dir) {
        long size = 0;
        for (File file : dir.listFiles()) {
            if (file != null && file.isDirectory()) {
                size += getDirSize(file);
            } else if (file != null && file.isFile()) {
                size += file.length();
            }
        }
        return size;
    }

    public static String readableFileSize(long size) {
        if (size <= 0) {
            return "0 Bytes";
        }
        String[] units = new String[]{"Bytes", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10((double) size) / Math.log10(1024.0d));
        StringBuilder stringBuilder = new StringBuilder();
        DecimalFormat decimalFormat = new DecimalFormat("#,##0.#");
        double d = (double) size;
        double pow = Math.pow(1024.0d, (double) digitGroups);
        Double.isNaN(d);
        stringBuilder.append(decimalFormat.format(d / pow));
        stringBuilder.append(" ");
        stringBuilder.append(units[digitGroups]);
        return stringBuilder.toString();
    }

    private void setupToolbar() {
        toolbarTitle.setText(getString(R.string.title_settings));
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

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onResume() {
        super.onResume();
        permissionVisibility();
    }

}

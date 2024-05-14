package com.app.matrixFM.activities;

import static com.app.matrixFM.utils.Constant.LOCALHOST_ADDRESS;
import static com.solodroid.ads.sdk.util.Constant.ADMOB;
import static com.solodroid.ads.sdk.util.Constant.AD_STATUS_ON;
import static com.solodroid.ads.sdk.util.Constant.GOOGLE_AD_MANAGER;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.multidex.MultiDex;

import com.app.matrixFM.Config;
import com.app.matrixFM.R;
import com.app.matrixFM.callbacks.CallbackConfig;
import com.app.matrixFM.database.prefs.AdsPref;
import com.app.matrixFM.database.prefs.SharedPref;
import com.app.matrixFM.models.Settings;
import com.app.matrixFM.rests.RestAdapter;
import com.app.matrixFM.utils.Constant;
import com.app.matrixFM.utils.Utils;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.messaging.FirebaseMessaging;
import com.solodroid.ads.sdk.format.AppOpenAdManager;
import com.solodroid.ads.sdk.format.AppOpenAdMob;
import com.solodroid.ads.sdk.util.OnShowAdCompleteListener;
import com.solodroid.ads.sdk.util.Tools;
import com.solodroid.push.sdk.provider.OneSignalPush;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyApplication extends Application implements Application.ActivityLifecycleCallbacks {

    public static final String TAG = "MyApplication";
    FirebaseAnalytics mFirebaseAnalytics;
    SharedPref sharedPref;
    AdsPref adsPref;
    private AppOpenAdMob appOpenAdMob;
    private AppOpenAdManager appOpenAdManager;
    Activity currentActivity;

    public MyApplication() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.registerActivityLifecycleCallbacks(this);
        sharedPref = new SharedPref(this);
        adsPref = new AdsPref(this);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(lifecycleObserver);
        appOpenAdMob = new AppOpenAdMob();
        appOpenAdManager = new AppOpenAdManager();
        initNotification();
    }

    public void initNotification() {
        new OneSignalPush.Builder(this)
                .setOneSignalAppId(getResources().getString(R.string.onesignal_app_id))
                .build(() -> {
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra(OneSignalPush.EXTRA_ID, OneSignalPush.Data.id);
                    intent.putExtra(OneSignalPush.EXTRA_TITLE, OneSignalPush.Data.title);
                    intent.putExtra(OneSignalPush.EXTRA_MESSAGE, OneSignalPush.Data.message);
                    intent.putExtra(OneSignalPush.EXTRA_IMAGE, OneSignalPush.Data.bigImage);
                    intent.putExtra(OneSignalPush.EXTRA_LAUNCH_URL, OneSignalPush.Data.launchUrl);
                    intent.putExtra(OneSignalPush.EXTRA_UNIQUE_ID, OneSignalPush.AdditionalData.uniqueId);
                    intent.putExtra(OneSignalPush.EXTRA_POST_ID, OneSignalPush.AdditionalData.postId);
                    intent.putExtra(OneSignalPush.EXTRA_LINK, OneSignalPush.AdditionalData.link);
                    startActivity(intent);
                });
        FirebaseMessaging.getInstance().subscribeToTopic(getResources().getString(R.string.fcm_notification_topic));
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    LifecycleObserver lifecycleObserver = new DefaultLifecycleObserver() {
        @Override
        public void onStart(@NonNull LifecycleOwner owner) {
            DefaultLifecycleObserver.super.onStart(owner);
            if (Constant.isAppOpen) {
                if (adsPref.getAdStatus().equals(AD_STATUS_ON)) {
                    switch (adsPref.getAdType()) {
                        case ADMOB:
                            if (!adsPref.getAdMobAppOpenAdId().equals("0")) {
                                if (!currentActivity.getIntent().hasExtra("unique_id")) {
                                    appOpenAdMob.showAdIfAvailable(currentActivity, adsPref.getAdMobAppOpenAdId());
                                }
                            }
                            break;
                        case GOOGLE_AD_MANAGER:
                            if (!adsPref.getAdManagerAppOpenAdId().equals("0")) {
                                if (!currentActivity.getIntent().hasExtra("unique_id")) {
                                    appOpenAdManager.showAdIfAvailable(currentActivity, adsPref.getAdManagerAppOpenAdId());
                                }
                            }
                            break;
                    }
                }
            }
        }
    };

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        if (adsPref.getAdStatus().equals(AD_STATUS_ON)) {
            switch (adsPref.getAdType()) {
                case ADMOB:
                    if (!adsPref.getAdMobAppOpenAdId().equals("0")) {
                        if (!appOpenAdMob.isShowingAd) {
                            currentActivity = activity;
                        }
                    }
                    break;
                case GOOGLE_AD_MANAGER:
                    if (!adsPref.getAdManagerAppOpenAdId().equals("0")) {
                        if (!appOpenAdManager.isShowingAd) {
                            currentActivity = activity;
                        }
                    }
                    break;
            }
        }
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
    }

    public void showAdIfAvailable(@NonNull Activity activity, @NonNull OnShowAdCompleteListener onShowAdCompleteListener) {
        // We wrap the showAdIfAvailable to enforce that other classes only interact with MyApplication class
        if (adsPref.getAdStatus().equals(AD_STATUS_ON)) {
            switch (adsPref.getAdType()) {
                case ADMOB:
                    if (!adsPref.getAdMobAppOpenAdId().equals("0")) {
                        appOpenAdMob.showAdIfAvailable(activity, adsPref.getAdMobAppOpenAdId(), onShowAdCompleteListener);
                        Constant.isAppOpen = true;
                    }
                    break;
                case GOOGLE_AD_MANAGER:
                    if (!adsPref.getAdManagerAppOpenAdId().equals("0")) {
                        appOpenAdManager.showAdIfAvailable(activity, adsPref.getAdManagerAppOpenAdId(), onShowAdCompleteListener);
                        Constant.isAppOpen = true;
                    }
                    break;
            }
        }
    }

}
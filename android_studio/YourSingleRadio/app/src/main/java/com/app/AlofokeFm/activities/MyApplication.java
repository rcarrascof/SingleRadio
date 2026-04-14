package com.app.AlofokeFm.activities;

import static com.solodroidx.ads.util.Constant.AD_STATUS_ON;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.multidex.MultiDex;

import com.app.AlofokeFm.R;
import com.app.AlofokeFm.database.prefs.AdsPref;
import com.app.AlofokeFm.database.prefs.SharedPref;
import com.app.AlofokeFm.utils.Constant;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.messaging.FirebaseMessaging;
import com.solodroid.push.sdk.provider.OneSignalPush;
import com.solodroidx.ads.appopen.AppOpenAd;
import com.solodroidx.ads.appopen.AppOpenAdManager;
import com.solodroidx.ads.appopen.AppOpenAdMob;

public class MyApplication extends Application {

    public static final String TAG = "MyApplication";
    FirebaseAnalytics mFirebaseAnalytics;
    SharedPref sharedPref;
    AdsPref adsPref;
    AppOpenAd appOpenAd;

    @Override
    public void onCreate() {
        super.onCreate();
        sharedPref = new SharedPref(this);
        adsPref = new AdsPref(this);
        initFirebase();
        initNotification();
        initOpenAds();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    private void initFirebase() {
        FirebaseApp.initializeApp(this);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
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

    private void initOpenAds() {
        adsPref = new AdsPref(this);
        appOpenAd = new AppOpenAd();
        if (adsPref.getAdStatus()) {
            registerActivityLifecycleCallbacks(activityLifecycleCallbacks);
            ProcessLifecycleOwner.get().getLifecycle().addObserver(lifecycleObserver);
            appOpenAd.initAppOpenAdMob(new AppOpenAdMob())
                    .initAppOpenAdManager(new AppOpenAdManager())
                    .setAdStatus(AD_STATUS_ON)
                    .setAdNetwork(adsPref.getMainAds())
                    .setBackupAdNetwork(adsPref.getBackupAds())
                    .setPlacementOnStart(Constant.APP_OPEN_AD_ON_START)
                    .setPlacementOnResume(Constant.APP_OPEN_AD_ON_RESUME)
                    .setAdMobAppOpenId(adsPref.getAdMobAppOpenAdId())
                    .setAdManagerAppOpenId(adsPref.getAdManagerAppOpenAdId());
        }
    }

    LifecycleObserver lifecycleObserver = new DefaultLifecycleObserver() {
        @Override
        public void onStart(@NonNull LifecycleOwner owner) {
            DefaultLifecycleObserver.super.onStart(owner);
            if (AppOpenAd.isAppOpenAdLoaded) {
                if (appOpenAd != null) {
                    appOpenAd.setOnStartLifecycleObserver();
                    Log.d("AppOpenAd", "appOpenAd no null, setOnStartLifecycleObserver");
                }
                Log.d("AppOpenAd", "onStart");
            } else {
                Log.d("AppOpenAd", "isAppOpenAdLoaded false");
            }
        }

        @Override
        public void onStop(@NonNull LifecycleOwner owner) {
            DefaultLifecycleObserver.super.onStop(owner);
//            Constant.isForeground = false;
//            Constant.isPausedFromClick = false;
        }
    };

    ActivityLifecycleCallbacks activityLifecycleCallbacks = new ActivityLifecycleCallbacks() {
        @Override
        public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        }

        @Override
        public void onActivityStarted(@NonNull Activity activity) {
            if (appOpenAd != null) {
                appOpenAd.setOnStartActivityLifecycleCallbacks(activity);
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
    };

    public void showAdIfAvailable(@NonNull Activity activity, @NonNull com.solodroidx.ads.listener.OnShowAdCompleteListener onShowAdCompleteListener) {
        if (appOpenAd != null) {
            appOpenAd.showAdIfAvailable(activity, onShowAdCompleteListener);
            Log.d("AppOpenAd", "status: " + AppOpenAd.isAppOpenAdLoaded);
        } else {
            onShowAdCompleteListener.onShowAdComplete();
        }
    }

}
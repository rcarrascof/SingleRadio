package com.app.AlofokeFm.activities;

import static com.app.AlofokeFm.utils.Constant.LOCALHOST_ADDRESS;
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
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.multidex.MultiDex;

import com.app.AlofokeFm.Config;
import com.app.AlofokeFm.callbacks.CallbackConfig;
import com.app.AlofokeFm.database.prefs.AdsPref;
import com.app.AlofokeFm.database.prefs.SharedPref;
import com.app.AlofokeFm.models.Settings;
import com.app.AlofokeFm.rests.RestAdapter;
import com.app.AlofokeFm.utils.Utils;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.messaging.FirebaseMessaging;
import com.onesignal.OneSignal;
import com.solodroid.ads.sdk.format.AppOpenAdManager;
import com.solodroid.ads.sdk.format.AppOpenAdMob;
import com.solodroid.ads.sdk.util.OnShowAdCompleteListener;
import com.solodroid.ads.sdk.util.Tools;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyApplication extends Application implements Application.ActivityLifecycleCallbacks, LifecycleObserver {

    public static final String TAG = "MyApplication";
    String message = "";
    String big_picture = "";
    String title = "";
    String link = "";
    long post_id = -1;
    long unique_id = -1;
    FirebaseAnalytics mFirebaseAnalytics;
    Call<CallbackConfig> callbackCall = null;
    Settings settings;
    SharedPref sharedPref;
    Activity activity;
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
        MobileAds.initialize(this, initializationStatus -> {
        });
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        appOpenAdMob = new AppOpenAdMob();
        appOpenAdManager = new AppOpenAdManager();

        initNotification();
    }

    private void initNotification() {
        OneSignal.disablePush(false);
        Log.d(TAG, "OneSignal Notification is enabled");

        // Enable verbose OneSignal logging to debug issues if needed.
        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE);
        OneSignal.initWithContext(this);

        if (Config.ENABLE_REMOTE_JSON) {
            requestConfig();
        } else {
            requestConfigFromAssets();
        }

        OneSignal.setNotificationOpenedHandler(
                result -> {
                    title = result.getNotification().getTitle();
                    message = result.getNotification().getBody();
                    big_picture = result.getNotification().getBigPicture();
                    Log.d(TAG, title + ", " + message + ", " + big_picture);
                    try {
                        unique_id = result.getNotification().getAdditionalData().getLong("unique_id");
                        post_id = result.getNotification().getAdditionalData().getLong("post_id");
                        link = result.getNotification().getAdditionalData().getString("link");
                        Log.d(TAG, post_id + ", " + unique_id);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra("title", title);
                    intent.putExtra("link", link);
                    startActivity(intent);
                });

        OneSignal.unsubscribeWhenNotificationsAreDisabled(true);
    }


    private void requestConfig() {
        String data = Tools.decode(Config.ACCESS_KEY);
        String[] results = data.split("_applicationId_");
        String remoteUrl = results[0].replace("http://localhost", LOCALHOST_ADDRESS);
        requestAPI(remoteUrl);
    }

    private void requestAPI(String remoteUrl) {
        if (remoteUrl.startsWith("http://") || remoteUrl.startsWith("https://")) {
            if (remoteUrl.contains("https://drive.google.com")) {
                String driveUrl = remoteUrl.replace("https://", "").replace("http://", "");
                List<String> data = Arrays.asList(driveUrl.split("/"));
                String googleDriveFileId = data.get(3);
                callbackCall = RestAdapter.createAPI().getDriveJsonFileId(googleDriveFileId);
                Log.d(TAG, "Request API from Google Drive Share link");
                Log.d(TAG, "Google drive file id : " + data.get(3));
            } else {
                callbackCall = RestAdapter.createAPI().getJsonUrl(remoteUrl);
                Log.d(TAG, "Request API from Json Url");
            }
        } else {
            callbackCall = RestAdapter.createAPI().getDriveJsonFileId(remoteUrl);
            Log.d(TAG, "Request API from Json Url");
        }
        callbackCall.enqueue(new Callback<CallbackConfig>() {
            @Override
            public void onResponse(@NonNull Call<CallbackConfig> call, @NonNull Response<CallbackConfig> response) {
                CallbackConfig resp = response.body();
                if (resp != null) {
                    settings = resp.settings.get(0);
                    FirebaseMessaging.getInstance().subscribeToTopic(settings.fcm_notification_topic);
                    OneSignal.setAppId(settings.onesignal_app_id);
                    Log.d(TAG, "FCM Subscribe topic : " + settings.fcm_notification_topic);
                    Log.d(TAG, "OneSignal App ID : " + settings.onesignal_app_id);
                }
            }

            @Override
            public void onFailure(@NonNull Call<CallbackConfig> call, @NonNull Throwable t) {
                Log.e("onFailure", "" + t.getMessage());
            }

        });
    }

    private void requestConfigFromAssets() {
        try {
            JSONObject jsonObject = new JSONObject(Objects.requireNonNull(Utils.loadJSONFromAsset(this, "config.json")));
            JSONArray settings = jsonObject.getJSONArray("settings");
            JSONObject setting = settings.getJSONObject(0);
            String onesignal_app_id = setting.getString("onesignal_app_id");
            String fcm_notification_topic = setting.getString("fcm_notification_topic");

            FirebaseMessaging.getInstance().subscribeToTopic(fcm_notification_topic);
            OneSignal.setAppId(onesignal_app_id);

        } catch (JSONException e) {
            e.printStackTrace();
            Log.d(TAG, "failed");
        }
    }

    public AppOpenAdManager getAppOpenAdManager() {
        return this.appOpenAdManager;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    protected void onMoveToForeground() {
        // Show the ad (if available) when the app moves to foreground.
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

    public void showAdIfAvailable(@NonNull Activity activity, @NonNull OnShowAdCompleteListener onShowAdCompleteListener) {
        // We wrap the showAdIfAvailable to enforce that other classes only interact with MyApplication class
        if (adsPref.getAdStatus().equals(AD_STATUS_ON)) {
            switch (adsPref.getAdType()) {
                case ADMOB:
                    if (!adsPref.getAdMobAppOpenAdId().equals("0")) {
                        appOpenAdMob.showAdIfAvailable(activity, adsPref.getAdMobAppOpenAdId(), onShowAdCompleteListener);
                    }
                    break;
                case GOOGLE_AD_MANAGER:
                    if (!adsPref.getAdManagerAppOpenAdId().equals("0")) {
                        appOpenAdManager.showAdIfAvailable(activity, adsPref.getAdManagerAppOpenAdId(), onShowAdCompleteListener);
                    }
                    break;
            }
        }
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {

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
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {

    }
}
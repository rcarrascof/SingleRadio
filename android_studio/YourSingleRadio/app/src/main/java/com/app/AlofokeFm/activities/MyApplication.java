package com.app.AlofokeFm.activities;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.multidex.MultiDex;

import com.app.AlofokeFm.Config;
import com.app.AlofokeFm.callbacks.CallbackConfig;
import com.app.AlofokeFm.database.prefs.SharedPref;
import com.app.AlofokeFm.models.Settings;
import com.app.AlofokeFm.rests.ApiInterface;
import com.app.AlofokeFm.rests.RestAdapter;
import com.facebook.ads.AudienceNetworkAds;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.messaging.FirebaseMessaging;
import com.onesignal.OneSignal;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyApplication extends Application {

    public static final String TAG = MyApplication.class.getSimpleName();
    private static MyApplication mInstance;
    private AppOpenAdManager appOpenAdManager;
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

    public MyApplication() {
        mInstance = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        sharedPref = new SharedPref(this);
        MobileAds.initialize(this, initializationStatus -> {
        });
        appOpenAdManager = new AppOpenAdManager.Builder(this).build();
        AudienceNetworkAds.initialize(this);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        OneSignal.disablePush(false);
        Log.d(TAG, "OneSignal Notification is enabled");

        // Enable verbose OneSignal logging to debug issues if needed.
        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE);
        OneSignal.initWithContext(this);
        requestConfig();

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
        ApiInterface apiInterface = RestAdapter.createAPI();
        callbackCall = apiInterface.getConfig(Config.JSON_URL);
        callbackCall.enqueue(new Callback<CallbackConfig>() {
            @Override
            public void onResponse(Call<CallbackConfig> call, Response<CallbackConfig> response) {
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
            public void onFailure(Call<CallbackConfig> call, Throwable t) {
                Log.e("onFailure", "" + t.getMessage());
            }

        });
    }

    public AppOpenAdManager getAppOpenAdManager() {
        return this.appOpenAdManager;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    public static synchronized MyApplication getInstance() {
        return mInstance;
    }

}
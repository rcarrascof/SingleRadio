package com.app.AlofokeFm.activities;

import static com.app.AlofokeFm.utils.Constant.LOCALHOST_ADDRESS;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.multidex.MultiDex;

import com.app.AlofokeFm.Config;
import com.app.AlofokeFm.callbacks.CallbackConfig;
import com.app.AlofokeFm.database.prefs.SharedPref;
import com.app.AlofokeFm.models.Settings;
import com.app.AlofokeFm.rests.RestAdapter;
import com.app.AlofokeFm.utils.Utils;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.messaging.FirebaseMessaging;
import com.onesignal.OneSignal;
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

public class MyApplication extends Application {

    public static final String TAG = "MyApplication";
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
    Activity activity;

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
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

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

    public static synchronized MyApplication getInstance() {
        return mInstance;
    }

}
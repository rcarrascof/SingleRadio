package com.app.AlofokeFm.activities;

import static com.app.AlofokeFm.utils.Constant.ADMOB;
import static com.app.AlofokeFm.utils.Constant.AD_STATUS_ON;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.app.AlofokeFm.Config;
import com.app.AlofokeFm.R;
import com.app.AlofokeFm.callbacks.CallbackConfig;
import com.app.AlofokeFm.database.dao.AppDatabase;
import com.app.AlofokeFm.database.dao.DAO;
import com.app.AlofokeFm.database.prefs.AdsPref;
import com.app.AlofokeFm.database.prefs.SharedPref;
import com.app.AlofokeFm.models.Ads;
import com.app.AlofokeFm.models.Radio;
import com.app.AlofokeFm.models.Settings;
import com.app.AlofokeFm.rests.ApiInterface;
import com.app.AlofokeFm.rests.RestAdapter;
import com.app.AlofokeFm.utils.Tools;
import com.google.android.gms.ads.FullScreenContentCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivitySplash extends AppCompatActivity {

    public static final String TAG = "ActivitySplash";
    AppOpenAdManager appOpenAdManager;
    private boolean isAdShown = false;
    private boolean isAdDismissed = false;
    private boolean isLoadCompleted = false;
    ProgressBar progressBar;
    AdsPref adsPref;
    SharedPref sharedPref;
    Call<CallbackConfig> callbackCall = null;
    Radio radio;
    Settings settings;
    Ads ads;
    private DAO db;
    long id = System.currentTimeMillis();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        db = AppDatabase.getDb(this).get();
        sharedPref = new SharedPref(this);

        adsPref = new AdsPref(this);

        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        if (Config.ENABLE_REMOTE_JSON) {
            requestConfig();
        } else {
            requestConfigFromAssets();
            readSocial();
        }

    }

    private void requestConfigFromAssets() {
        try {
            JSONObject jsonObject = new JSONObject(Objects.requireNonNull(Tools.loadJSONFromAsset(this, "config.json")));
            JSONArray radios = jsonObject.getJSONArray("radio");
            JSONArray settings = jsonObject.getJSONArray("settings");
            JSONArray ads = jsonObject.getJSONArray("ads");
            JSONArray socials = jsonObject.getJSONArray("socials");

            JSONObject radio = radios.getJSONObject(0);
            JSONObject setting = settings.getJSONObject(0);
            JSONObject ad = ads.getJSONObject(0);

            String radio_name = radio.getString("radio_name");
            String radio_genre = radio.getString("radio_genre");
            String radio_url = radio.getString("radio_url");
            String radio_image_url = radio.getString("radio_image_url");
            String background_image_url = radio.getString("background_image_url");

            String onesignal_app_id = setting.getString("onesignal_app_id");
            String fcm_notification_topic = setting.getString("fcm_notification_topic");
            String privacy_policy_url = setting.getString("privacy_policy_url");
            String more_apps_url = setting.getString("more_apps_url");

            String ad_status = ad.getString("ad_status");
            String ad_type = ad.getString("ad_type");
            String admob_publisher_id = ad.getString("admob_publisher_id");
            String admob_banner_unit_id = ad.getString("admob_banner_unit_id");
            String admob_interstitial_unit_id = ad.getString("admob_interstitial_unit_id");
            String admob_native_unit_id = ad.getString("admob_native_unit_id");
            String admob_app_open_unit_id = ad.getString("admob_app_open_unit_id");
            String startapp_app_id = ad.getString("startapp_app_id");
            String unity_game_id = ad.getString("unity_game_id");
            String unity_banner_placement_id = ad.getString("unity_banner_placement_id");
            String unity_interstitial_placement_id = ad.getString("unity_interstitial_placement_id");
            String applovin_banner_unit_id = ad.getString("applovin_banner_unit_id");
            String applovin_interstitial_unit_id = ad.getString("applovin_interstitial_unit_id");
            int interstitial_ad_interval = ad.getInt("interstitial_ad_interval");

            sharedPref.setPrivacyPolicyUrl(privacy_policy_url);
            sharedPref.setMoreAppsUrl(more_apps_url);

            adsPref.saveAds(
                    ad_status,
                    ad_type,
                    admob_publisher_id,
                    admob_banner_unit_id,
                    admob_interstitial_unit_id,
                    admob_native_unit_id,
                    admob_app_open_unit_id,
                    startapp_app_id,
                    unity_game_id,
                    unity_banner_placement_id,
                    unity_interstitial_placement_id,
                    applovin_banner_unit_id,
                    applovin_interstitial_unit_id,
                    interstitial_ad_interval
            );

            db.deleteAllRadio();
            new Handler().postDelayed(() -> {
                db.insertRadio(
                        id,
                        radio_name,
                        radio_genre,
                        radio_url,
                        radio_image_url,
                        background_image_url
                );
                onSplashFinished();
            }, 100);

            Log.d(TAG, "success");

        } catch (JSONException e) {
            e.printStackTrace();
            Log.d(TAG, "failed");
            onSplashFinished();
        }
    }

    private void requestConfig() {
        ApiInterface apiInterface = RestAdapter.createAPI();
        callbackCall = apiInterface.getConfig(Config.JSON_URL);
        callbackCall.enqueue(new Callback<CallbackConfig>() {
            @Override
            public void onResponse(Call<CallbackConfig> call, Response<CallbackConfig> response) {
                CallbackConfig resp = response.body();
                if (resp != null) {
                    radio = resp.radio.get(0);
                    settings = resp.settings.get(0);
                    sharedPref.setPrivacyPolicyUrl(settings.privacy_policy_url);
                    sharedPref.setMoreAppsUrl(settings.more_apps_url);

                    ads = resp.ads.get(0);
                    adsPref.saveAds(
                            ads.ad_status,
                            ads.ad_type,
                            ads.admob_publisher_id,
                            ads.admob_banner_unit_id,
                            ads.admob_interstitial_unit_id,
                            ads.admob_native_unit_id,
                            ads.admob_app_open_unit_id,
                            ads.startapp_app_id,
                            ads.unity_game_id,
                            ads.unity_banner_placement_id,
                            ads.unity_interstitial_placement_id,
                            ads.applovin_banner_unit_id,
                            ads.applovin_interstitial_unit_id,
                            ads.interstitial_ad_interval
                    );
                    db.deleteAllRadio();
                    new Handler().postDelayed(() -> {
                        db.insertRadio(
                                id,
                                radio.radio_name,
                                radio.radio_genre,
                                radio.radio_url,
                                radio.radio_image_url,
                                radio.background_image_url
                        );
                        onSplashFinished();
                    }, 100);
                    Log.d(TAG, "success load config");
                } else {
                    requestConfigFromAssets();
                }
            }

            @Override
            public void onFailure(Call<CallbackConfig> call, Throwable t) {
                requestConfigFromAssets();
            }

        });
    }

    private void onSplashFinished() {
        if (adsPref.getAdType().equals(ADMOB) && adsPref.getAdStatus().equals(AD_STATUS_ON)) {
            if (!adsPref.getAdMobAppOpenId().equals("")) {
                launchAppOpenAd();
            } else {
                launchMainScreen();
            }
        } else {
            launchMainScreen();
        }
    }

    private void launchAppOpenAd() {
        appOpenAdManager = ((MyApplication) getApplication()).getAppOpenAdManager();
        loadResources();
        appOpenAdManager.showAdIfAvailable(new FullScreenContentCallback() {

            @Override
            public void onAdShowedFullScreenContent() {
                isAdShown = true;
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                isAdDismissed = true;
                if (isLoadCompleted) {
                    launchMainScreen();
                    Log.d(TAG, "isLoadCompleted and launch main screen...");
                } else {
                    Log.d(TAG, "Waiting resources to be loaded...");
                }
            }
        });
    }

    private void loadResources() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            isLoadCompleted = true;
            // Check whether App Open ad was shown or not.
            if (isAdShown) {
                // Check App Open ad was dismissed or not.
                if (isAdDismissed) {
                    launchMainScreen();
                    Log.d(TAG, "isAdDismissed and launch main screen...");
                } else {
                    Log.d(TAG, "Waiting for ad to be dismissed...");
                }
                Log.d(TAG, "Ad shown...");
            } else {
                launchMainScreen();
                Log.d(TAG, "Ad not shown...");
            }
        }, 200);
    }

    private void launchMainScreen() {
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }, Config.SPLASH_DURATION);
    }

    private void readSocial() {
        try {
            JSONObject object = new JSONObject(readJSON());
            JSONArray array = object.getJSONArray("socials");
            for (int i = 0; i < array.length(); i++) {
                JSONObject jsonObject = array.getJSONObject(i);
                String social_name = jsonObject.getString("social_name");
                String social_icon = jsonObject.getString("social_icon");
                String social_url = jsonObject.getString("social_url");
                db.deleteAllSocial();
                new Handler().postDelayed(() -> db.insertSocial(social_name, social_icon, social_url), 100);
                Log.d(TAG, social_name);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String readJSON() {
        String json = null;
        try {
            // Opening data.json file
            InputStream inputStream = getAssets().open("config.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            // read values in the byte array
            inputStream.read(buffer);
            inputStream.close();
            // convert byte to string
            json = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return json;
        }
        return json;
    }

}

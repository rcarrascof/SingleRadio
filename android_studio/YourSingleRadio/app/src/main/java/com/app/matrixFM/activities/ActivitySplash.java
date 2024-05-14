package com.app.matrixFM.activities;

import static com.app.matrixFM.utils.Constant.LOCALHOST_ADDRESS;
import static com.solodroid.ads.sdk.util.Constant.ADMOB;
import static com.solodroid.ads.sdk.util.Constant.AD_STATUS_ON;
import static com.solodroid.ads.sdk.util.Constant.GOOGLE_AD_MANAGER;

import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.app.matrixFM.BuildConfig;
import com.app.matrixFM.Config;
import com.app.matrixFM.R;
import com.app.matrixFM.callbacks.CallbackConfig;
import com.app.matrixFM.database.dao.AppDatabase;
import com.app.matrixFM.database.dao.DAO;
import com.app.matrixFM.database.prefs.AdsPref;
import com.app.matrixFM.database.prefs.SharedPref;
import com.app.matrixFM.models.Ads;
import com.app.matrixFM.models.Radio;
import com.app.matrixFM.models.Settings;
import com.app.matrixFM.rests.RestAdapter;
import com.app.matrixFM.utils.AdsManager;
import com.app.matrixFM.utils.Utils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.solodroid.ads.sdk.util.Tools;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivitySplash extends AppCompatActivity {

    public static final String TAG = "ActivitySplash";
    ProgressBar progressBar;
    AdsPref adsPref;
    SharedPref sharedPref;
    AdsManager adsManager;
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
        adsManager = new AdsManager(this);
        adsManager.initializeAd();
        sharedPref = new SharedPref(this);
        adsPref = new AdsPref(this);

        new Handler(Looper.getMainLooper()).postDelayed(this::initAppConfiguration, Config.DELAY_SPLASH);

        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);


    }

    private void initAppConfiguration() {
        if (Config.ENABLE_REMOTE_JSON) {
            requestConfig();
        } else {
            requestConfigFromAssets();
            readSocial();
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void requestConfig() {
        if (Config.ACCESS_KEY.contains("XXXXX")) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("App not configured")
                    .setMessage("Please put your Access Key in your admin panel to Config, you can see the documentation for more detailed instructions.")
                    .setPositiveButton(getString(R.string.option_ok), (dialogInterface, i) -> finish())
                    .setCancelable(false)
                    .show();
        } else {
            String data = Tools.decode(Config.ACCESS_KEY);
            String[] results = data.split("_applicationId_");
            String remoteUrl = results[0].replace("http://localhost", LOCALHOST_ADDRESS);
            String applicationId = results[1];

            if (applicationId.equals(BuildConfig.APPLICATION_ID)) {
                requestConfig(remoteUrl);
            } else {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Error")
                        .setMessage("Whoops! invalid access key or applicationId, please check your configuration")
                        .setPositiveButton("Ok", (dialog, which) -> finish())
                        .setCancelable(false)
                        .show();
            }
            Log.d(TAG, "Start request config from url : " + remoteUrl);
        }
    }

    private void requestConfig(String remoteUrl) {

        if (remoteUrl.startsWith("http://") || remoteUrl.startsWith("https://")) {
            if (remoteUrl.contains("https://drive.google.com")) {
                String driveUrl = remoteUrl.replace("https://", "").replace("http://", "");
                List<String> data = Arrays.asList(driveUrl.split("/"));
                String googleDriveFileId = data.get(3);
                callbackCall = RestAdapter.createAPI().getDriveJsonFileId(googleDriveFileId);
            } else {
                callbackCall = RestAdapter.createAPI().getJsonUrl(remoteUrl);
            }
        } else {
            callbackCall = RestAdapter.createAPI().getDriveJsonFileId(remoteUrl);
        }

        callbackCall.enqueue(new Callback<CallbackConfig>() {
            @Override
            public void onResponse(@NonNull Call<CallbackConfig> call, @NonNull Response<CallbackConfig> response) {
                CallbackConfig resp = response.body();
                displayApiResults(resp);
            }

            @Override
            public void onFailure(@NonNull Call<CallbackConfig> call, @NonNull Throwable t) {
                requestConfigFromAssets();
                Log.d(TAG, "onFailure : " + t.getMessage());
            }

        });
    }

    private void displayApiResults(CallbackConfig resp) {
        if (resp != null) {

            radio = resp.radio.get(0);
            settings = resp.settings.get(0);
            ads = resp.ads.get(0);

            sharedPref.saveSettings(
                    settings.app_status,
                    settings.privacy_policy_url,
                    settings.more_apps_url,
                    settings.redirect_url,
                    radio.song_metadata,
                    radio.image_album_art,
                    radio.image_album_art_dynamic_background,
                    radio.blur_radio_background,
                    radio.auto_play
            );
            adsManager.saveAds(adsPref, ads);

            if (settings.app_status.equals("0")) {
                Intent intent = new Intent(getApplicationContext(), ActivityRedirect.class);
                intent.putExtra("redirect_url", settings.redirect_url);
                startActivity(intent);
                finish();
                Log.d(TAG, "App status is inactive, open redirect activity");
            } else {
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
                    showOpenAdsIfAvailable();
                }, 100);
            }
            Log.d(TAG, "success load config");
        } else {
            requestConfigFromAssets();
            Log.d(TAG, "error load remote json, start load config.json from assets");
        }
    }

    private void requestConfigFromAssets() {
        try {
            JSONObject jsonObject = new JSONObject(Objects.requireNonNull(Utils.loadJSONFromAsset(this, "config.json")));
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
            String blur_radio_background = radio.getString("blur_radio_background");
            String song_metadata = radio.getString("song_metadata");
            String image_album_art = radio.getString("image_album_art");
            String image_album_art_dynamic_background = radio.getString("image_album_art_dynamic_background");
            String auto_play = radio.getString("auto_play");

            String app_status = setting.getString("app_status");
            String privacy_policy_url = setting.getString("privacy_policy_url");
            String more_apps_url = setting.getString("more_apps_url");
            String redirect_url = setting.getString("redirect_url");

            String ad_status = ad.getString("ad_status");
            String ad_type = ad.getString("ad_type");
            String backup_ads = ad.getString("backup_ads");
            String admob_publisher_id = ad.getString("admob_publisher_id");
            String admob_banner_unit_id = ad.getString("admob_banner_unit_id");
            String admob_interstitial_unit_id = ad.getString("admob_interstitial_unit_id");
            String admob_native_unit_id = ad.getString("admob_native_unit_id");
            String admob_app_open_ad_unit_id = ad.getString("admob_app_open_ad_unit_id");
            String ad_manager_banner_unit_id = ad.getString("ad_manager_banner_unit_id");
            String ad_manager_interstitial_unit_id = ad.getString("ad_manager_interstitial_unit_id");
            String ad_manager_native_unit_id = ad.getString("ad_manager_native_unit_id");
            String ad_manager_app_open_ad_unit_id = ad.getString("ad_manager_app_open_ad_unit_id");
            String fan_banner_unit_id = ad.getString("fan_banner_unit_id");
            String fan_interstitial_unit_id = ad.getString("fan_interstitial_unit_id");
            String fan_native_unit_id = ad.getString("fan_native_unit_id");
            String startapp_app_id = ad.getString("startapp_app_id");
            String unity_game_id = ad.getString("unity_game_id");
            String unity_banner_placement_id = ad.getString("unity_banner_placement_id");
            String unity_interstitial_placement_id = ad.getString("unity_interstitial_placement_id");
            String applovin_banner_ad_unit_id = ad.getString("applovin_banner_ad_unit_id");
            String applovin_interstitial_ad_unit_id = ad.getString("applovin_interstitial_ad_unit_id");
            String applovin_native_ad_manual_unit_id = ad.getString("applovin_native_ad_manual_unit_id");
            String applovin_banner_zone_id = ad.getString("applovin_banner_zone_id");
            String applovin_interstitial_zone_id = ad.getString("applovin_interstitial_zone_id");
            String ironsource_app_key = ad.getString("ironsource_app_key");
            String ironsource_banner_placement_name = ad.getString("ironsource_banner_placement_name");
            String ironsource_interstitial_placement_name = ad.getString("ironsource_interstitial_placement_name");
            int interstitial_ad_interval = ad.getInt("interstitial_ad_interval");

            sharedPref.saveSettings(
                    app_status,
                    privacy_policy_url,
                    more_apps_url,
                    redirect_url,
                    song_metadata,
                    image_album_art,
                    image_album_art_dynamic_background,
                    blur_radio_background,
                    auto_play
            );

            adsPref.saveAds(
                    ad_status.replace("on", "1"),
                    ad_type,
                    backup_ads,
                    admob_publisher_id,
                    admob_banner_unit_id,
                    admob_interstitial_unit_id,
                    admob_native_unit_id,
                    admob_app_open_ad_unit_id,
                    ad_manager_banner_unit_id,
                    ad_manager_interstitial_unit_id,
                    ad_manager_native_unit_id,
                    ad_manager_app_open_ad_unit_id,
                    fan_banner_unit_id,
                    fan_interstitial_unit_id,
                    fan_native_unit_id,
                    startapp_app_id,
                    unity_game_id,
                    unity_banner_placement_id,
                    unity_interstitial_placement_id,
                    applovin_banner_ad_unit_id,
                    applovin_interstitial_ad_unit_id,
                    applovin_native_ad_manual_unit_id,
                    applovin_banner_zone_id,
                    applovin_interstitial_zone_id,
                    ironsource_app_key,
                    ironsource_banner_placement_name,
                    ironsource_interstitial_placement_name,
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
                showOpenAdsIfAvailable();
            }, 100);

            Log.d(TAG, "success");

        } catch (JSONException e) {
            e.printStackTrace();
            Log.d(TAG, "failed : " + e.getMessage());
            showOpenAdsIfAvailable();
        }
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

    /** @noinspection ResultOfMethodCallIgnored*/
    public String readJSON() {
        String json = null;
        try {
            InputStream inputStream = getAssets().open("config.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            json = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return json;
        }
        return json;
    }


    private void showOpenAdsIfAvailable() {
        if (adsPref.getAdStatus().equals(AD_STATUS_ON)) {
            Application application = getApplication();
            if (adsPref.getAdType().equals(ADMOB)) {
                if (!adsPref.getAdMobAppOpenAdId().equals("0")) {
                    ((MyApplication) application).showAdIfAvailable(ActivitySplash.this, this::startMainActivity);
                } else {
                    startMainActivity();
                }
            } else if (adsPref.getAdType().equals(GOOGLE_AD_MANAGER)) {
                if (!adsPref.getAdManagerAppOpenAdId().equals("0")) {
                    ((MyApplication) application).showAdIfAvailable(ActivitySplash.this, this::startMainActivity);
                } else {
                    startMainActivity();
                }
            } else {
                startMainActivity();
            }
        } else {
            startMainActivity();
        }
    }

    private void startMainActivity() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
        finish();
    }

}
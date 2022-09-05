package com.app.AlofokeFm.database.prefs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.core.content.ContextCompat;

import com.app.AlofokeFm.R;

public class SharedPref {

    private Context context;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private static final String IS_FIRST_TIME_LAUNCH = "IsFirstTimeLaunch";

    @SuppressLint("CommitPrefEdits")
    public SharedPref(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences("setting", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public int getFirstColor() {
        return sharedPreferences.getInt("first", ContextCompat.getColor(context, R.color.colorPrimaryDark));
    }

    public int getSecondColor() {
        return sharedPreferences.getInt("second", ContextCompat.getColor(context, R.color.colorPrimary));
    }

    public void setCheckSleepTime() {
        if (getSleepTime() <= System.currentTimeMillis()) {
            setSleepTime(false, 0, 0);
        }
    }

    public void setSleepTime(Boolean isTimerOn, long sleepTime, int id) {
        editor.putBoolean("isTimerOn", isTimerOn);
        editor.putLong("sleepTime", sleepTime);
        editor.putInt("sleepTimeID", id);
        editor.apply();
    }

    public Boolean getIsSleepTimeOn() {
        return sharedPreferences.getBoolean("isTimerOn", false);
    }

    public long getSleepTime() {
        return sharedPreferences.getLong("sleepTime", 0);
    }

    public int getSleepID() {
        return sharedPreferences.getInt("sleepTimeID", 0);
    }

    public void setFirstTimeLaunch(boolean isFirstTime) {
        editor.putBoolean(IS_FIRST_TIME_LAUNCH, isFirstTime);
        editor.apply();
    }

    public boolean isFirstTimeLaunch() {
        return sharedPreferences.getBoolean(IS_FIRST_TIME_LAUNCH, true);
    }

    public void setPrivacyPolicyUrl(String privacy_policy_url) {
        editor.putString("privacy_policy_url", privacy_policy_url);
        editor.apply();
    }

    public String getPrivacyPolicyUrl() {
        return sharedPreferences.getString("privacy_policy_url", "");
    }

    public void setMoreAppsUrl(String more_apps_url) {
        editor.putString("more_apps_url", more_apps_url);
        editor.apply();
    }

    public String getMoreAppsUrl() {
        return sharedPreferences.getString("more_apps_url", "");
    }

    public Integer getInAppReviewToken() {
        return sharedPreferences.getInt("in_app_review_token", 0);
    }

    public void updateInAppReviewToken(int value) {
        editor.putInt("in_app_review_token", value);
        editor.apply();
    }

    public String getSongMetadata() {
        return sharedPreferences.getString("song_metadata", "false");
    }

    public String getAutoPlay() {
        return sharedPreferences.getString("auto_play", "false");
    }

    public String getRedirectUrl() {
        return sharedPreferences.getString("redirect_url", "");
    }


    public void saveSettings(String app_status, String privacy_policy_url, String more_apps_url, String redirect_url, String song_metadata, String image_album_art, String auto_play) {
        editor.putString("app_status", app_status);
        editor.putString("privacy_policy_url", privacy_policy_url);
        editor.putString("more_apps_url", more_apps_url);
        editor.putString("redirect_url", redirect_url);
        editor.putString("song_metadata", song_metadata);
        editor.putString("image_album_art", image_album_art);
        editor.putString("auto_play", auto_play);
        editor.apply();
    }


    public String getImageAlbumArt() {
        return sharedPreferences.getString("image_album_art", "false");
    }
}
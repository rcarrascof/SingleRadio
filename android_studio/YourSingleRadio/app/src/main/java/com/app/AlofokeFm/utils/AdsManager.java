package com.app.AlofokeFm.utils;

import static com.solodroidx.ads.util.Constant.IRONSOURCE;

import android.app.Activity;
import android.content.Context;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.AlofokeFm.Config;
import com.app.AlofokeFm.R;
import com.app.AlofokeFm.database.prefs.AdsPref;
import com.app.AlofokeFm.database.prefs.SharedPref;
import com.app.AlofokeFm.models.Ads;
import com.solodroidx.ads.appopen.AppOpenAd;
import com.solodroidx.ads.banner.BannerAd;
import com.solodroidx.ads.gdpr.GDPR;
import com.solodroidx.ads.initialization.InitializeAd;
import com.solodroidx.ads.interstitial.InterstitialAd;
import com.solodroidx.ads.nativead.NativeAd;
import com.solodroidx.ads.nativead.NativeAdView;
import com.solodroidx.ads.nativead.NativeAdViewHolder;

public class AdsManager {

    Activity activity;
    InitializeAd initializeAd;
    AppOpenAd appOpenAd;
    BannerAd bannerAd;
    InterstitialAd interstitialAd;
    NativeAd nativeAd;
    NativeAdView nativeAdView;
    SharedPref sharedPref;
    AdsPref adsPref;
    GDPR gdpr;

    public AdsManager(Activity activity) {
        this.activity = activity;
        this.sharedPref = new SharedPref(activity);
        this.adsPref = new AdsPref(activity);
        this.gdpr = new GDPR(activity);
        initializeAd = new InitializeAd(activity);
        appOpenAd = new AppOpenAd(activity);
        bannerAd = new BannerAd(activity);
        interstitialAd = new InterstitialAd(activity);
        nativeAd = new NativeAd(activity);
        nativeAdView = new NativeAdView(activity);
    }

    public void initializeAd() {
        if (adsPref.getAdStatus()) {
            initializeAd.setAdStatus("1")
                    .setAdNetwork(adsPref.getMainAds())
                    .setBackupAdNetwork(adsPref.getBackupAds())
                    .setAppLovinSdkKey(activity.getResources().getString(R.string.applovin_sdk_key))
                    .setStartappAppId(adsPref.getStartappAppId())
                    .setUnityGameId(adsPref.getUnityGameId())
                    .setIronSourceAppKey(adsPref.getIronSourceAppKey())
                    .setDebug(Tools.isDebug())
                    .setApplicationId(Tools.getApplicationId())
                    .build();
        }
    }

    public void loadBannerAd(boolean placement) {
        if (placement) {
            if (adsPref.getAdStatus()) {
                bannerAd.setAdStatus("1")
                        .setAdNetwork(adsPref.getMainAds())
                        .setBackupAdNetwork(adsPref.getBackupAds())
                        .setAdMobBannerId(adsPref.getAdMobBannerId())
                        .setGoogleAdManagerBannerId(adsPref.getAdManagerBannerId())
                        .setFanBannerId(adsPref.getFanBannerUnitId())
                        .setUnityBannerId(adsPref.getUnityBannerPlacementId())
                        .setAppLovinBannerId(adsPref.getAppLovinBannerAdUnitId())
                        .setAppLovinBannerZoneId(adsPref.getAppLovinBannerZoneId())
                        .setIronSourceBannerId(adsPref.getIronSourceBannerId())
                        .setIsCollapsibleBanner(false)
                        .build();
            }
        }
    }

    public void destroyBannerAd() {
        if (adsPref.getAdStatus()) {
            bannerAd.destroyAndDetachBanner();
        }
    }

    public void resumeBannerAd(boolean placement) {
        if (adsPref.getAdStatus() && !adsPref.getIronSourceBannerId().equals("0")) {
            if (adsPref.getMainAds().equals(IRONSOURCE) || adsPref.getBackupAds().equals(IRONSOURCE)) {
                loadBannerAd(placement);
            }
        }
    }

    public void loadInterstitialAd(boolean placement, int interval) {
        if (placement) {
            if (adsPref.getAdStatus()) {
                interstitialAd.setAdStatus("1")
                        .setAdNetwork(adsPref.getMainAds())
                        .setBackupAdNetwork(adsPref.getBackupAds())
                        .setAdMobInterstitialId(adsPref.getAdMobInterstitialId())
                        .setGoogleAdManagerInterstitialId(adsPref.getAdManagerInterstitialId())
                        .setFanInterstitialId(adsPref.getFanInterstitialUnitId())
                        .setUnityInterstitialId(adsPref.getUnityInterstitialPlacementId())
                        .setAppLovinInterstitialId(adsPref.getAppLovinInterstitialAdUnitId())
                        .setAppLovinInterstitialZoneId(adsPref.getAppLovinInterstitialZoneId())
                        .setIronSourceInterstitialId(adsPref.getIronSourceInterstitialId())
                        .setInterval(interval)
                        .build();
            }
        }
    }

    public void showInterstitialAd() {
        if (adsPref.getAdStatus()) {
            interstitialAd.show();
        }
    }

    public void loadNativeAd(boolean placement, String style) {
        if (placement) {
            if (adsPref.getAdStatus()) {
                nativeAd.setAdStatus("1")
                        .setAdNetwork(adsPref.getMainAds())
                        .setBackupAdNetwork(adsPref.getBackupAds())
                        .setAdMobNativeId(adsPref.getAdMobNativeId())
                        .setAdManagerNativeId(adsPref.getAdManagerNativeId())
                        .setFanNativeId(adsPref.getFanNativeUnitId())
                        .setAppLovinNativeId(adsPref.getAppLovinNativeAdManualUnitId())
                        .setNativeAdStyle(style)
                        .setRadius(R.dimen.corner_radius)
                        .setStrokeWidth(R.dimen.native_ad_stroke_width)
                        .setStrokeColor(R.color.color_stroke_native_ad)
                        .setBackgroundColor(R.color.color_native_ad_background, R.color.color_native_ad_background)
                        .setMargin(R.dimen.no_margin, R.dimen.no_margin, R.dimen.no_margin, R.dimen.no_margin)
                        .build();
            }
        }
    }

    public RecyclerView.ViewHolder createNativeAdViewHolder(Context context, @NonNull ViewGroup parent) {
        String adStatus;
        if (adsPref.getAdStatus()) {
            if (Constant.NATIVE_AD_DRAWER_MENU) {
                adStatus = "1";
            } else {
                adStatus = "0";
            }
        } else {
            adStatus = "0";
        }

        int noMargin = R.dimen.no_margin;
        int marginEnd = R.dimen.padding_medium;

        return new NativeAdViewHolder(NativeAdViewHolder.setLayoutInflater(parent, Tools.nativeAdStyleFormatter(Constant.NATIVE_AD_STYLE_DRAWER_MENU)))
                .setAdStatus(adStatus)
                .setAdNetwork(adsPref.getMainAds())
                .setBackupAdNetwork(adsPref.getBackupAds())
                .setAdMobNativeId(adsPref.getAdMobNativeId())
                .setAdManagerNativeId(adsPref.getAdManagerNativeId())
                .setFanNativeId(adsPref.getFanNativeUnitId())
                .setAppLovinNativeId(adsPref.getAppLovinNativeAdManualUnitId())
                .setNativeAdStyle(Tools.nativeAdStyleFormatter(Constant.NATIVE_AD_STYLE_DRAWER_MENU))
                .setBackgroundColor(R.color.color_native_ad_background, R.color.color_native_ad_background)
                .setRadius(context, R.dimen.corner_radius)
                .setStrokeWidth(context, R.dimen.native_ad_stroke_width)
                .setStrokeColor(context, R.color.color_stroke_native_ad)
                .setMargin(context, noMargin, noMargin, noMargin, marginEnd);
    }

    public void bindNativeAdViewHolder(Context context, NativeAdViewHolder holder) {
        holder.buildNativeAd(context);
    }

    public void updateConsentStatus() {
        if (Config.ENABLE_GDPR_UMP_SDK) {
            gdpr.updateGDPRConsentStatus(adsPref.getMainAds(), false, false);
        }
    }

    public void saveAds(AdsPref adsPref, Ads ads) {
        adsPref.saveAds(
                ads.ad_status.equals("1"),
                ads.ad_type,
                ads.backup_ads,
                ads.admob_publisher_id,
                ads.admob_banner_unit_id,
                ads.admob_interstitial_unit_id,
                ads.admob_native_unit_id,
                ads.admob_app_open_ad_unit_id,
                ads.ad_manager_banner_unit_id,
                ads.ad_manager_interstitial_unit_id,
                ads.ad_manager_native_unit_id,
                ads.ad_manager_app_open_ad_unit_id,
                ads.fan_banner_unit_id,
                ads.fan_interstitial_unit_id,
                ads.fan_native_unit_id,
                ads.startapp_app_id,
                ads.unity_game_id,
                ads.unity_banner_placement_id,
                ads.unity_interstitial_placement_id,
                ads.applovin_banner_ad_unit_id,
                ads.applovin_interstitial_ad_unit_id,
                ads.applovin_native_ad_manual_unit_id,
                ads.applovin_banner_zone_id,
                ads.applovin_interstitial_zone_id,
                ads.ironsource_app_key,
                ads.ironsource_banner_placement_name,
                ads.ironsource_interstitial_placement_name,
                ads.interstitial_ad_interval
        );
    }

}

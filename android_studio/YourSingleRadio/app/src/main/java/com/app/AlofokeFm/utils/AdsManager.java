package com.app.AlofokeFm.utils;

import static com.solodroid.ads.sdk.util.Constant.AD_STATUS_ON;
import static com.solodroid.ads.sdk.util.Constant.IRONSOURCE;

import android.app.Activity;
import android.view.View;

import com.app.AlofokeFm.BuildConfig;
import com.app.AlofokeFm.Config;
import com.app.AlofokeFm.R;
import com.app.AlofokeFm.database.prefs.AdsPref;
import com.app.AlofokeFm.database.prefs.SharedPref;
import com.app.AlofokeFm.models.Ads;
import com.solodroid.ads.sdk.format.AdNetwork;
import com.solodroid.ads.sdk.format.BannerAd;
import com.solodroid.ads.sdk.format.InterstitialAd;
import com.solodroid.ads.sdk.format.NativeAdView;
import com.solodroid.ads.sdk.gdpr.GDPR;

public class AdsManager {

    Activity activity;
    AdNetwork.Initialize adNetwork;
    BannerAd.Builder bannerAd;
    InterstitialAd.Builder interstitialAd;
    NativeAdView.Builder nativeAdView;
    SharedPref sharedPref;
    AdsPref adsPref;
    GDPR gdpr;

    public AdsManager(Activity activity) {
        this.activity = activity;
        this.sharedPref = new SharedPref(activity);
        this.adsPref = new AdsPref(activity);
        this.gdpr = new GDPR(activity);
        adNetwork = new AdNetwork.Initialize(activity);
        bannerAd = new BannerAd.Builder(activity);
        interstitialAd = new InterstitialAd.Builder(activity);
        nativeAdView = new NativeAdView.Builder(activity);
    }

    public void initializeAd() {
        adNetwork.setAdStatus(adsPref.getAdStatus())
                .setAdNetwork(adsPref.getAdType())
                .setBackupAdNetwork(adsPref.getBackupAds())
                .setStartappAppId(adsPref.getStartappAppId())
                .setUnityGameId(adsPref.getUnityGameId())
                .setIronSourceAppKey(adsPref.getIronSourceAppKey())
                .setDebug(BuildConfig.DEBUG)
                .build();
    }

    public void loadBannerAd(boolean placement) {
        if (placement) {
            bannerAd.setAdStatus(adsPref.getAdStatus())
                    .setAdNetwork(adsPref.getAdType())
                    .setBackupAdNetwork(adsPref.getBackupAds())
                    .setAdMobBannerId(adsPref.getAdMobBannerId())
                    .setGoogleAdManagerBannerId(adsPref.getAdManagerBannerId())
                    .setFanBannerId(adsPref.getFanBannerUnitId())
                    .setUnityBannerId(adsPref.getUnityBannerPlacementId())
                    .setAppLovinBannerId(adsPref.getAppLovinBannerAdUnitId())
                    .setAppLovinBannerZoneId(adsPref.getAppLovinBannerZoneId())
                    .setIronSourceBannerId(adsPref.getIronSourceBannerId())
                    .setPlacementStatus(1)
                    .build();
        }
    }

    public void loadInterstitialAd(boolean placement, int interval) {
        if (placement) {
            interstitialAd.setAdStatus(adsPref.getAdStatus())
                    .setAdNetwork(adsPref.getAdType())
                    .setBackupAdNetwork(adsPref.getBackupAds())
                    .setAdMobInterstitialId(adsPref.getAdMobInterstitialId())
                    .setGoogleAdManagerInterstitialId(adsPref.getAdManagerInterstitialId())
                    .setFanInterstitialId(adsPref.getFanInterstitialUnitId())
                    .setUnityInterstitialId(adsPref.getUnityInterstitialPlacementId())
                    .setAppLovinInterstitialId(adsPref.getAppLovinInterstitialAdUnitId())
                    .setAppLovinInterstitialZoneId(adsPref.getAppLovinInterstitialZoneId())
                    .setIronSourceInterstitialId(adsPref.getIronSourceInterstitialId())
                    .setInterval(interval)
                    .setPlacementStatus(1)
                    .build();
        }
    }

    public void loadNativeAdView(View view, boolean placement, String style) {
        if (placement) {
            nativeAdView.setAdStatus(adsPref.getAdStatus())
                    .setAdNetwork(adsPref.getAdType())
                    .setBackupAdNetwork(adsPref.getBackupAds())
                    .setAdMobNativeId(adsPref.getAdMobNativeId())
                    .setAdManagerNativeId(adsPref.getAdManagerNativeId())
                    .setFanNativeId(adsPref.getFanNativeUnitId())
                    .setAppLovinNativeId(adsPref.getAppLovinNativeAdManualUnitId())
                    .setAppLovinDiscoveryMrecZoneId(adsPref.getAppLovinBannerZoneId())
                    .setPlacementStatus(1)
                    .setNativeAdStyle(style)
                    .setView(view)
                    .setNativeAdBackgroundColor(R.color.color_native_ad_background, R.color.color_native_ad_background)
                    .setPadding(
                            activity.getResources().getDimensionPixelSize(R.dimen.padding_small),
                            activity.getResources().getDimensionPixelSize(R.dimen.padding_small),
                            activity.getResources().getDimensionPixelSize(R.dimen.padding_small),
                            activity.getResources().getDimensionPixelSize(R.dimen.padding_small)
                    )
                    .setMargin(
                            activity.getResources().getDimensionPixelSize(R.dimen.no_margin),
                            activity.getResources().getDimensionPixelSize(R.dimen.margin_small),
                            activity.getResources().getDimensionPixelSize(R.dimen.no_margin),
                            activity.getResources().getDimensionPixelSize(R.dimen.margin_small)
                    )
                    .build();
        }
    }

    public void showInterstitialAd() {
        interstitialAd.show();
    }

    public void destroyBannerAd() {
        bannerAd.destroyAndDetachBanner();
    }

    public void resumeBannerAd(boolean placement) {
        if (adsPref.getAdStatus().equals(AD_STATUS_ON) && !adsPref.getIronSourceBannerId().equals("0")) {
            if (adsPref.getAdType().equals(IRONSOURCE) || adsPref.getBackupAds().equals(IRONSOURCE)) {
                loadBannerAd(placement);
            }
        }
    }

    public void updateConsentStatus() {
        if (Config.ENABLE_GDPR_UMP_SDK) {
            gdpr.updateGDPRConsentStatus(adsPref.getAdType(), false, false);
        }
    }

    public void saveAds(AdsPref adsPref, Ads ads) {
        adsPref.saveAds(
                ads.ad_status.replace("on", "1"),
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

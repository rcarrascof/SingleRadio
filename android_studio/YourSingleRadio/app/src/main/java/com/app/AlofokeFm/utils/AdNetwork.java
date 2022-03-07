package com.app.AlofokeFm.utils;

import static com.app.AlofokeFm.utils.Constant.ADMOB;
import static com.app.AlofokeFm.utils.Constant.AD_STATUS_ON;
import static com.app.AlofokeFm.utils.Constant.APPLOVIN;
import static com.app.AlofokeFm.utils.Constant.STARTAPP;
import static com.app.AlofokeFm.utils.Constant.UNITY;
import static com.app.AlofokeFm.utils.Constant.UNITY_ADS_BANNER_HEIGHT;
import static com.app.AlofokeFm.utils.Constant.UNITY_ADS_BANNER_WIDTH;

import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.app.AlofokeFm.R;
import com.app.AlofokeFm.database.prefs.AdsPref;
import com.app.AlofokeFm.database.prefs.SharedPref;
import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdListener;
import com.applovin.mediation.MaxAdViewAdListener;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.ads.MaxAdView;
import com.applovin.mediation.ads.MaxInterstitialAd;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.MediaView;
import com.startapp.sdk.ads.banner.Banner;
import com.startapp.sdk.ads.banner.BannerListener;
import com.startapp.sdk.ads.nativead.NativeAdDetails;
import com.startapp.sdk.ads.nativead.NativeAdPreferences;
import com.startapp.sdk.ads.nativead.StartAppNativeAd;
import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.adlisteners.AdEventListener;
import com.unity3d.ads.IUnityAdsShowListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.services.banners.BannerErrorInfo;
import com.unity3d.services.banners.BannerView;
import com.unity3d.services.banners.UnityBannerSize;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class AdNetwork {

    private static final String TAG = "AdNetwork";
    private final Activity context;
    SharedPref sharedPref;
    AdsPref adsPref;

    //Banner
    private FrameLayout adContainerView;
    private AdView adView;
    RelativeLayout startAppAdView;

    //Interstitial
    private InterstitialAd adMobInterstitialAd;
    private StartAppAd startAppAd;
    private MaxInterstitialAd maxInterstitialAd;
    private int retryAttempt;
    private int counter = 1;

    //Native
    MediaView mediaView;
    TemplateView admob_native_ad;
    LinearLayout admob_native_background;
    View startapp_native_ad;
    ImageView startapp_native_image;
    TextView startapp_native_title;
    TextView startapp_native_description;
    Button startapp_native_button;
    LinearLayout startapp_native_background;

    public AdNetwork(Activity context) {
        this.context = context;
        this.sharedPref = new SharedPref(context);
        this.adsPref = new AdsPref(context);
    }

    public void loadBannerAdNetwork(int ad_placement) {
        if (adsPref.getAdStatus().equals(AD_STATUS_ON) && ad_placement != 0) {
            switch (adsPref.getAdType()) {
                case ADMOB:
                    adContainerView = context.findViewById(R.id.admob_banner_view_container);
                    adContainerView.post(() -> {
                        adView = new AdView(context);
                        adView.setAdUnitId(adsPref.getAdMobBannerId());
                        adContainerView.removeAllViews();
                        adContainerView.addView(adView);
                        adView.setAdSize(Tools.getAdSize(context));
                        adView.loadAd(Tools.getAdRequest(context));
                        adView.setAdListener(new AdListener() {
                            @Override
                            public void onAdLoaded() {
                                // Code to be executed when an ad finishes loading.
                                adContainerView.setVisibility(View.VISIBLE);
                            }

                            @Override
                            public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                                // Code to be executed when an ad request fails.
                                adContainerView.setVisibility(View.GONE);
                            }

                            @Override
                            public void onAdOpened() {
                                // Code to be executed when an ad opens an overlay that
                                // covers the screen.
                            }

                            @Override
                            public void onAdClicked() {
                                // Code to be executed when the user clicks on an ad.
                            }

                            @Override
                            public void onAdClosed() {
                                // Code to be executed when the user is about to return
                                // to the app after tapping on an ad.
                            }
                        });
                    });
                    break;
                case STARTAPP:
                    startAppAdView = context.findViewById(R.id.startapp_banner_view_container);
                    Banner banner = new Banner(context, new BannerListener() {
                        @Override
                        public void onReceiveAd(View banner) {
                            startAppAdView.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onFailedToReceiveAd(View banner) {
                            startAppAdView.setVisibility(View.GONE);
                        }

                        @Override
                        public void onImpression(View view) {

                        }

                        @Override
                        public void onClick(View banner) {
                        }
                    });
                    startAppAdView.addView(banner);
                    break;
                case UNITY:
                    RelativeLayout unityAdView = context.findViewById(R.id.unity_banner_view_container);
                    BannerView bottomBanner = new BannerView(context, adsPref.getUnityBannerPlacementId(), new UnityBannerSize(UNITY_ADS_BANNER_WIDTH, UNITY_ADS_BANNER_HEIGHT));
                    bottomBanner.setListener(new BannerView.IListener() {
                        @Override
                        public void onBannerLoaded(BannerView bannerView) {
                            unityAdView.setVisibility(View.VISIBLE);
                            Log.d("Unity_banner", "ready");
                        }

                        @Override
                        public void onBannerClick(BannerView bannerView) {

                        }

                        @Override
                        public void onBannerFailedToLoad(BannerView bannerView, BannerErrorInfo bannerErrorInfo) {
                            Log.d("SupportTest", "Banner Error" + bannerErrorInfo);
                            unityAdView.setVisibility(View.GONE);
                        }

                        @Override
                        public void onBannerLeftApplication(BannerView bannerView) {

                        }
                    });
                    unityAdView.addView(bottomBanner);
                    bottomBanner.load();
                    break;
                case APPLOVIN:
                    RelativeLayout appLovinAdView = context.findViewById(R.id.applovin_banner_view_container);
                    MaxAdView maxAdView = new MaxAdView(adsPref.getAppLovinBannerId(), context);
                    maxAdView.setListener(new MaxAdViewAdListener() {
                        @Override
                        public void onAdExpanded(MaxAd ad) {

                        }

                        @Override
                        public void onAdCollapsed(MaxAd ad) {

                        }

                        @Override
                        public void onAdLoaded(MaxAd ad) {
                            appLovinAdView.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onAdDisplayed(MaxAd ad) {

                        }

                        @Override
                        public void onAdHidden(MaxAd ad) {

                        }

                        @Override
                        public void onAdClicked(MaxAd ad) {

                        }

                        @Override
                        public void onAdLoadFailed(String adUnitId, MaxError error) {
                            appLovinAdView.setVisibility(View.GONE);
                        }

                        @Override
                        public void onAdDisplayFailed(MaxAd ad, MaxError error) {

                        }
                    });

                    int width = ViewGroup.LayoutParams.MATCH_PARENT;
                    int heightPx = context.getResources().getDimensionPixelSize(R.dimen.applovin_banner_height);
                    maxAdView.setLayoutParams(new FrameLayout.LayoutParams(width, heightPx));
                    maxAdView.setBackgroundColor(context.getResources().getColor(R.color.white));
                    appLovinAdView.addView(maxAdView);
                    maxAdView.loadAd();
                    break;
            }
        }
    }

    public void loadInterstitialAdNetwork(int ad_placement) {
        if (adsPref.getAdStatus().equals(AD_STATUS_ON) && ad_placement != 0) {
            switch (adsPref.getAdType()) {
                case ADMOB:
                    InterstitialAd.load(context, adsPref.getAdMobInterstitialId(), Tools.getAdRequest(context), new InterstitialAdLoadCallback() {
                        @Override
                        public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                            adMobInterstitialAd = interstitialAd;
                            adMobInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                                @Override
                                public void onAdDismissedFullScreenContent() {
                                    loadInterstitialAdNetwork(ad_placement);
                                }

                                @Override
                                public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                                    Log.d(TAG, "The ad failed to show.");
                                }

                                @Override
                                public void onAdShowedFullScreenContent() {
                                    adMobInterstitialAd = null;
                                    Log.d(TAG, "The ad was shown.");
                                }
                            });
                            Log.i(TAG, "onAdLoaded");
                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            Log.i(TAG, loadAdError.getMessage());
                            adMobInterstitialAd = null;
                            Log.d(TAG, "Failed load AdMob Interstitial Ad");
                        }
                    });

                    break;
                case STARTAPP:
                    startAppAd = new StartAppAd(context);

                    break;
                case APPLOVIN:
                    maxInterstitialAd = new MaxInterstitialAd(adsPref.getAppLovinInterstitialId(), context);
                    maxInterstitialAd.setListener(new MaxAdListener() {
                        @Override
                        public void onAdLoaded(MaxAd ad) {
                            retryAttempt = 0;
                            Log.d(TAG, "AppLovin Interstitial Ad loaded...");
                        }

                        @Override
                        public void onAdDisplayed(MaxAd ad) {
                        }

                        @Override
                        public void onAdHidden(MaxAd ad) {
                            maxInterstitialAd.loadAd();
                        }

                        @Override
                        public void onAdClicked(MaxAd ad) {

                        }

                        @Override
                        public void onAdLoadFailed(String adUnitId, MaxError error) {
                            retryAttempt++;
                            long delayMillis = TimeUnit.SECONDS.toMillis((long) Math.pow(2, Math.min(6, retryAttempt)));
                            new Handler().postDelayed(() -> maxInterstitialAd.loadAd(), delayMillis);
                            Log.d(TAG, "failed to load AppLovin Interstitial");
                        }

                        @Override
                        public void onAdDisplayFailed(MaxAd ad, MaxError error) {
                            maxInterstitialAd.loadAd();
                        }
                    });

                    // Load the first ad
                    maxInterstitialAd.loadAd();
                    break;
            }
        }
    }

    public void showInterstitialAdNetwork(int ad_placement, int interval) {
        if (adsPref.getAdStatus().equals(AD_STATUS_ON) && ad_placement != 0) {
            switch (adsPref.getAdType()) {
                case ADMOB:
                    if (adMobInterstitialAd != null) {
                        if (counter == interval) {
                            adMobInterstitialAd.show(context);
                            counter = 1;
                        } else {
                            counter++;
                        }
                    }
                    break;
                case STARTAPP:
                    if (counter == interval) {
                        startAppAd.showAd();
                        counter = 1;
                    } else {
                        counter++;
                    }
                    break;
                case UNITY:
                    if (UnityAds.isReady(adsPref.getUnityInterstitialPlacementId())) {
                        if (counter == interval) {
                            UnityAds.show(context, adsPref.getUnityInterstitialPlacementId(), new IUnityAdsShowListener() {
                                @Override
                                public void onUnityAdsShowFailure(String s, UnityAds.UnityAdsShowError unityAdsShowError, String s1) {

                                }

                                @Override
                                public void onUnityAdsShowStart(String s) {

                                }

                                @Override
                                public void onUnityAdsShowClick(String s) {

                                }

                                @Override
                                public void onUnityAdsShowComplete(String s, UnityAds.UnityAdsShowCompletionState unityAdsShowCompletionState) {

                                }
                            });
                            counter = 1;
                        } else {
                            counter++;
                        }
                    }
                    break;
                case APPLOVIN:
                    Log.d(TAG, "selected");
                    if (maxInterstitialAd.isReady()) {
                        Log.d(TAG, "ready : " + counter);
                        if (counter == interval) {
                            maxInterstitialAd.showAd();
                            counter = 1;
                            Log.d(TAG, "show ad");
                        } else {
                            counter++;
                        }
                    }
                    break;
            }
        }
    }

    public void loadNativeAdNetwork(int ad_placement) {

        if (adsPref.getAdStatus().equals(AD_STATUS_ON) && ad_placement != 0) {

            admob_native_ad = context.findViewById(R.id.admob_native_ad_container);
            mediaView = context.findViewById(R.id.media_view);
            admob_native_background = context.findViewById(R.id.background);
            startapp_native_ad = context.findViewById(R.id.startapp_native_ad_container);
            startapp_native_image = context.findViewById(R.id.startapp_native_image);
            startapp_native_title = context.findViewById(R.id.startapp_native_title);
            startapp_native_description = context.findViewById(R.id.startapp_native_description);
            startapp_native_button = context.findViewById(R.id.startapp_native_button);
            startapp_native_button.setOnClickListener(v1 -> startapp_native_ad.performClick());
            startapp_native_background = context.findViewById(R.id.startapp_native_background);

            switch (adsPref.getAdType()) {
                case ADMOB:
                    if (admob_native_ad.getVisibility() != View.VISIBLE) {
                        AdLoader adLoader = new AdLoader.Builder(context, adsPref.getAdMobNativeId())
                                .forNativeAd(NativeAd -> {

                                    ColorDrawable colorDrawable = new ColorDrawable(ContextCompat.getColor(context, R.color.white));
                                    NativeTemplateStyle styles = new NativeTemplateStyle.Builder().withMainBackgroundColor(colorDrawable).build();
                                    admob_native_ad.setStyles(styles);
                                    admob_native_background.setBackgroundResource(R.color.white);

                                    mediaView.setImageScaleType(ImageView.ScaleType.CENTER_CROP);
                                    admob_native_ad.setNativeAd(NativeAd);
                                    admob_native_ad.setVisibility(View.VISIBLE);
                                })
                                .withAdListener(new AdListener() {
                                    @Override
                                    public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                                        admob_native_ad.setVisibility(View.GONE);
                                    }
                                })
                                .build();
                        adLoader.loadAd(Tools.getAdRequest(context));
                    } else {
                        Log.d("NATIVE_AD", "AdMob native ads has been loaded");
                    }
                    break;

                case STARTAPP:
                    if (startapp_native_ad.getVisibility() != View.VISIBLE) {
                        StartAppNativeAd startAppNativeAd = new StartAppNativeAd(context);
                        NativeAdPreferences nativePrefs = new NativeAdPreferences()
                                .setAdsNumber(3)
                                .setAutoBitmapDownload(true)
                                .setPrimaryImageSize(Constant.STARTAPP_IMAGE_MEDIUM);
                        AdEventListener adListener = new AdEventListener() {
                            @Override
                            public void onReceiveAd(com.startapp.sdk.adsbase.Ad arg0) {
                                Log.d("STARTAPP_ADS", "ad loaded");
                                startapp_native_ad.setVisibility(View.VISIBLE);
                                //noinspection rawtypes
                                ArrayList ads = startAppNativeAd.getNativeAds(); // get NativeAds list

                                // Print all ads details to log
                                for (Object ad : ads) {
                                    Log.d("STARTAPP_ADS", ad.toString());
                                }

                                NativeAdDetails ad = (NativeAdDetails) ads.get(0);
                                if (ad != null) {
                                    startapp_native_image.setImageBitmap(ad.getImageBitmap());
                                    startapp_native_title.setText(ad.getTitle());
                                    startapp_native_description.setText(ad.getDescription());
                                    startapp_native_button.setText(ad.isApp() ? "Install" : "Open");
                                    ad.registerViewForInteraction(startapp_native_ad);
                                }

                                startapp_native_background.setBackgroundResource(R.color.white);

                            }

                            @Override
                            public void onFailedToReceiveAd(com.startapp.sdk.adsbase.Ad arg0) {
                                startapp_native_ad.setVisibility(View.GONE);
                                Log.d("STARTAPP_ADS", "ad failed");
                            }
                        };
                        startAppNativeAd.loadAd(nativePrefs, adListener);
                    } else {
                        Log.d("NATIVE_AD", "StartApp native ads has been loaded");
                    }
                    break;

            }

        }

    }

}
